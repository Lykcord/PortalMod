package net.portalmod.common.commands;

import static org.junit.Assert.*;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

import net.minecraft.network.FriendlyByteBuf;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

public class LowercaseEnumArgumentTest {
    private enum Choice {
        PRIMARY,
        SECONDARY
    }

    @Test
    public void parsesAndSuggestsLowercaseEnumNames() throws Exception {
        LowercaseEnumArgument<Choice> argument = LowercaseEnumArgument.enumArgument(Choice.class);
        assertEquals(Choice.PRIMARY, argument.parse(new StringReader("primary")));
        assertEquals(
                List.of("secondary"),
                argument.listSuggestions(null, new SuggestionsBuilder("sec", 0)).get().getList().stream()
                        .map(suggestion -> suggestion.getText())
                        .toList());
        assertThrows(CommandSyntaxException.class, () -> argument.parse(new StringReader("PRIMARY")));
    }

    @Test
    public void enumNamesDoNotDependOnTheSystemLocale() throws Exception {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(
                    Choice.PRIMARY,
                    LowercaseEnumArgument.enumArgument(Choice.class).parse(new StringReader("primary")));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void serializesTheArgumentTemplateForRegisteredEnums() throws Exception {
        LowercaseEnumArgument.Serializer serializer = new LowercaseEnumArgument.Serializer(Choice.class);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            serializer.serializeToNetwork(serializer.unpack(LowercaseEnumArgument.enumArgument(Choice.class)), buffer);
            assertEquals(
                    Choice.SECONDARY,
                    serializer.deserializeFromNetwork(buffer).instantiate(null).parse(new StringReader("secondary")));
        } finally {
            buffer.release();
        }
    }

    @Test
    public void rejectsUnregisteredNetworkTypesInsteadOfLoadingClasses() {
        LowercaseEnumArgument.Serializer serializer = new LowercaseEnumArgument.Serializer(Choice.class);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeUtf("java.lang.System");
            assertThrows(DecoderException.class, () -> serializer.deserializeFromNetwork(buffer));
        } finally {
            buffer.release();
        }
    }
}
