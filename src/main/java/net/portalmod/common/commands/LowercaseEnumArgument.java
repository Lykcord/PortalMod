package net.portalmod.common.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import io.netty.handler.codec.DecoderException;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class LowercaseEnumArgument<T extends Enum<T>> implements ArgumentType<T> {
    private static final Dynamic2CommandExceptionType INVALID_ENUM =
            new Dynamic2CommandExceptionType(
                    (found, constants) ->
                            Component.translatable("argument.enum.invalid", found).append(" " + constants));

    private final Class<T> enumClass;
    private final Map<String, T> options;

    public static <R extends Enum<R>> LowercaseEnumArgument<R> enumArgument(Class<R> enumClass) {
        return new LowercaseEnumArgument<>(enumClass);
    }

    private LowercaseEnumArgument(final Class<T> enumClass) {
        this.enumClass = enumClass;
        this.options = new HashMap<>();
        this.options.putAll(
                Arrays.stream(enumClass.getEnumConstants())
                        .collect(Collectors.toMap(option -> option.name().toLowerCase(Locale.ROOT), option -> option)));
    }

    @Override
    public T parse(final StringReader reader) throws CommandSyntaxException {
        String argument = reader.readUnquotedString();

        if (this.options.containsKey(argument)) {
            return this.options.get(argument);
        } else {
            throw INVALID_ENUM.createWithContext(reader, argument, Arrays.toString(this.options.keySet().toArray()));
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
            final CommandContext<S> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(this.options.keySet(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return this.options.keySet();
    }

    public static final class Serializer implements ArgumentTypeInfo<LowercaseEnumArgument<?>, Serializer.Template> {
        private final Map<String, Class<? extends Enum<?>>> enums;

        @SafeVarargs
        public Serializer(Class<? extends Enum<?>>... enumClasses) {
            Map<String, Class<? extends Enum<?>>> registered = new HashMap<>();
            for (Class<? extends Enum<?>> enumClass : enumClasses) {
                registered.put(enumClass.getName(), enumClass);
            }
            enums = Map.copyOf(registered);
        }

        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf buffer) {
            buffer.writeUtf(template.enumClass.getName(), 256);
        }

        @Override
        public Template deserializeFromNetwork(FriendlyByteBuf buffer) {
            String name = buffer.readUtf(256);
            Class<? extends Enum<?>> enumClass = enums.get(name);
            if (enumClass == null) {
                throw new DecoderException("Unknown PortalMod command enum: " + name);
            }
            return new Template(enumClass);
        }

        @Override
        public void serializeToJson(Template template, JsonObject json) {
            json.addProperty("enum", template.enumClass.getName());
        }

        @Override
        public Template unpack(LowercaseEnumArgument<?> argument) {
            if (enums.get(argument.enumClass.getName()) != argument.enumClass) {
                throw new IllegalArgumentException(
                        "Unregistered PortalMod command enum: " + argument.enumClass.getName());
            }
            return new Template(argument.enumClass);
        }

        public final class Template implements ArgumentTypeInfo.Template<LowercaseEnumArgument<?>> {
            private final Class<? extends Enum<?>> enumClass;

            private Template(Class<? extends Enum<?>> enumClass) {
                this.enumClass = enumClass;
            }

            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public LowercaseEnumArgument<?> instantiate(CommandBuildContext context) {
                return enumArgument((Class) enumClass);
            }

            @Override
            public ArgumentTypeInfo<LowercaseEnumArgument<?>, ?> type() {
                return Serializer.this;
            }
        }
    }
}
