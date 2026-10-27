package net.portalmod.core.math;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public class Mat4 {
    public static Mat4 identity() {
        return new Mat4(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
    }

    protected double m00, m01, m02, m03;
    protected double m10, m11, m12, m13;
    protected double m20, m21, m22, m23;
    protected double m30, m31, m32, m33;

    public Mat4(
            double m00,
            double m01,
            double m02,
            double m03,
            double m10,
            double m11,
            double m12,
            double m13,
            double m20,
            double m21,
            double m22,
            double m23,
            double m30,
            double m31,
            double m32,
            double m33) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;
        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;
    }

    public Mat4(double[] array) {
        this.m00 = array[0];
        this.m01 = array[1];
        this.m02 = array[2];
        this.m03 = array[3];
        this.m10 = array[4];
        this.m11 = array[5];
        this.m12 = array[6];
        this.m13 = array[7];
        this.m20 = array[8];
        this.m21 = array[9];
        this.m22 = array[10];
        this.m23 = array[11];
        this.m30 = array[12];
        this.m31 = array[13];
        this.m32 = array[14];
        this.m33 = array[15];
    }

    public Mat4(Quaternionf rotation) {
        float x = rotation.x();
        float y = rotation.y();
        float z = rotation.z();
        float w = rotation.w();
        float twiceXSquared = 2.0F * x * x;
        float twiceYSquared = 2.0F * y * y;
        float twiceZSquared = 2.0F * z * z;
        this.m00 = 1.0F - twiceYSquared - twiceZSquared;
        this.m11 = 1.0F - twiceZSquared - twiceXSquared;
        this.m22 = 1.0F - twiceXSquared - twiceYSquared;
        this.m33 = 1.0F;
        float xy = x * y;
        float yz = y * z;
        float zx = z * x;
        float xw = x * w;
        float yw = y * w;
        float zw = z * w;
        this.m10 = 2.0F * (xy + zw);
        this.m01 = 2.0F * (xy - zw);
        this.m20 = 2.0F * (zx - yw);
        this.m02 = 2.0F * (zx + yw);
        this.m21 = 2.0F * (yz + xw);
        this.m12 = 2.0F * (yz - xw);
    }

    public Mat4(Matrix4fc m) {
        this.m00 = m.m00();
        this.m01 = m.m10();
        this.m02 = m.m20();
        this.m03 = m.m30();
        this.m10 = m.m01();
        this.m11 = m.m11();
        this.m12 = m.m21();
        this.m13 = m.m31();
        this.m20 = m.m02();
        this.m21 = m.m12();
        this.m22 = m.m22();
        this.m23 = m.m32();
        this.m30 = m.m03();
        this.m31 = m.m13();
        this.m32 = m.m23();
        this.m33 = m.m33();
    }

    public void store(FloatBuffer buffer) {
        buffer.put(0, (float) this.m00);
        buffer.put(1, (float) this.m01);
        buffer.put(2, (float) this.m02);
        buffer.put(3, (float) this.m03);
        buffer.put(4, (float) this.m10);
        buffer.put(5, (float) this.m11);
        buffer.put(6, (float) this.m12);
        buffer.put(7, (float) this.m13);
        buffer.put(8, (float) this.m20);
        buffer.put(9, (float) this.m21);
        buffer.put(10, (float) this.m22);
        buffer.put(11, (float) this.m23);
        buffer.put(12, (float) this.m30);
        buffer.put(13, (float) this.m31);
        buffer.put(14, (float) this.m32);
        buffer.put(15, (float) this.m33);
    }

    public double adjugateAndDet() {
        double upperMinor01 = this.m00 * this.m11 - this.m01 * this.m10;
        double upperMinor02 = this.m00 * this.m12 - this.m02 * this.m10;
        double upperMinor03 = this.m00 * this.m13 - this.m03 * this.m10;
        double upperMinor12 = this.m01 * this.m12 - this.m02 * this.m11;
        double upperMinor13 = this.m01 * this.m13 - this.m03 * this.m11;
        double upperMinor23 = this.m02 * this.m13 - this.m03 * this.m12;
        double lowerMinor01 = this.m20 * this.m31 - this.m21 * this.m30;
        double lowerMinor02 = this.m20 * this.m32 - this.m22 * this.m30;
        double lowerMinor03 = this.m20 * this.m33 - this.m23 * this.m30;
        double lowerMinor12 = this.m21 * this.m32 - this.m22 * this.m31;
        double lowerMinor13 = this.m21 * this.m33 - this.m23 * this.m31;
        double lowerMinor23 = this.m22 * this.m33 - this.m23 * this.m32;
        double adjugate00 = this.m11 * lowerMinor23 - this.m12 * lowerMinor13 + this.m13 * lowerMinor12;
        double adjugate10 = -this.m10 * lowerMinor23 + this.m12 * lowerMinor03 - this.m13 * lowerMinor02;
        double adjugate20 = this.m10 * lowerMinor13 - this.m11 * lowerMinor03 + this.m13 * lowerMinor01;
        double adjugate30 = -this.m10 * lowerMinor12 + this.m11 * lowerMinor02 - this.m12 * lowerMinor01;
        double adjugate01 = -this.m01 * lowerMinor23 + this.m02 * lowerMinor13 - this.m03 * lowerMinor12;
        double adjugate11 = this.m00 * lowerMinor23 - this.m02 * lowerMinor03 + this.m03 * lowerMinor02;
        double adjugate21 = -this.m00 * lowerMinor13 + this.m01 * lowerMinor03 - this.m03 * lowerMinor01;
        double adjugate31 = this.m00 * lowerMinor12 - this.m01 * lowerMinor02 + this.m02 * lowerMinor01;
        double adjugate02 = this.m31 * upperMinor23 - this.m32 * upperMinor13 + this.m33 * upperMinor12;
        double adjugate12 = -this.m30 * upperMinor23 + this.m32 * upperMinor03 - this.m33 * upperMinor02;
        double adjugate22 = this.m30 * upperMinor13 - this.m31 * upperMinor03 + this.m33 * upperMinor01;
        double adjugate32 = -this.m30 * upperMinor12 + this.m31 * upperMinor02 - this.m32 * upperMinor01;
        double adjugate03 = -this.m21 * upperMinor23 + this.m22 * upperMinor13 - this.m23 * upperMinor12;
        double adjugate13 = this.m20 * upperMinor23 - this.m22 * upperMinor03 + this.m23 * upperMinor02;
        double adjugate23 = -this.m20 * upperMinor13 + this.m21 * upperMinor03 - this.m23 * upperMinor01;
        double adjugate33 = this.m20 * upperMinor12 - this.m21 * upperMinor02 + this.m22 * upperMinor01;
        this.m00 = adjugate00;
        this.m10 = adjugate10;
        this.m20 = adjugate20;
        this.m30 = adjugate30;
        this.m01 = adjugate01;
        this.m11 = adjugate11;
        this.m21 = adjugate21;
        this.m31 = adjugate31;
        this.m02 = adjugate02;
        this.m12 = adjugate12;
        this.m22 = adjugate22;
        this.m32 = adjugate32;
        this.m03 = adjugate03;
        this.m13 = adjugate13;
        this.m23 = adjugate23;
        this.m33 = adjugate33;
        return upperMinor01 * lowerMinor23
                - upperMinor02 * lowerMinor13
                + upperMinor03 * lowerMinor12
                + upperMinor12 * lowerMinor03
                - upperMinor13 * lowerMinor02
                + upperMinor23 * lowerMinor01;
    }

    public Mat4 transpose() {
        double temporary = this.m10;
        this.m10 = this.m01;
        this.m01 = temporary;
        temporary = this.m20;
        this.m20 = this.m02;
        this.m02 = temporary;
        temporary = this.m21;
        this.m21 = this.m12;
        this.m12 = temporary;
        temporary = this.m30;
        this.m30 = this.m03;
        this.m03 = temporary;
        temporary = this.m31;
        this.m31 = this.m13;
        this.m13 = temporary;
        temporary = this.m32;
        this.m32 = this.m23;
        this.m23 = temporary;

        return this;
    }

    public boolean invert() {
        double determinant = this.adjugateAndDet();
        if (Math.abs(determinant) > 1.0E-6F) {
            this.mul(determinant);
            return true;
        } else {
            return false;
        }
    }

    private void mul(double scale) {
        this.m00 *= scale;
        this.m01 *= scale;
        this.m02 *= scale;
        this.m03 *= scale;
        this.m10 *= scale;
        this.m11 *= scale;
        this.m12 *= scale;
        this.m13 *= scale;
        this.m20 *= scale;
        this.m21 *= scale;
        this.m22 *= scale;
        this.m23 *= scale;
        this.m30 *= scale;
        this.m31 *= scale;
        this.m32 *= scale;
        this.m33 *= scale;
    }

    public Mat4 mul(Mat4 m) {
        double m00 = this.m00 * m.m00 + this.m01 * m.m10 + this.m02 * m.m20 + this.m03 * m.m30;
        double m01 = this.m00 * m.m01 + this.m01 * m.m11 + this.m02 * m.m21 + this.m03 * m.m31;
        double m02 = this.m00 * m.m02 + this.m01 * m.m12 + this.m02 * m.m22 + this.m03 * m.m32;
        double m03 = this.m00 * m.m03 + this.m01 * m.m13 + this.m02 * m.m23 + this.m03 * m.m33;
        double m10 = this.m10 * m.m00 + this.m11 * m.m10 + this.m12 * m.m20 + this.m13 * m.m30;
        double m11 = this.m10 * m.m01 + this.m11 * m.m11 + this.m12 * m.m21 + this.m13 * m.m31;
        double m12 = this.m10 * m.m02 + this.m11 * m.m12 + this.m12 * m.m22 + this.m13 * m.m32;
        double m13 = this.m10 * m.m03 + this.m11 * m.m13 + this.m12 * m.m23 + this.m13 * m.m33;
        double m20 = this.m20 * m.m00 + this.m21 * m.m10 + this.m22 * m.m20 + this.m23 * m.m30;
        double m21 = this.m20 * m.m01 + this.m21 * m.m11 + this.m22 * m.m21 + this.m23 * m.m31;
        double m22 = this.m20 * m.m02 + this.m21 * m.m12 + this.m22 * m.m22 + this.m23 * m.m32;
        double m23 = this.m20 * m.m03 + this.m21 * m.m13 + this.m22 * m.m23 + this.m23 * m.m33;
        double m30 = this.m30 * m.m00 + this.m31 * m.m10 + this.m32 * m.m20 + this.m33 * m.m30;
        double m31 = this.m30 * m.m01 + this.m31 * m.m11 + this.m32 * m.m21 + this.m33 * m.m31;
        double m32 = this.m30 * m.m02 + this.m31 * m.m12 + this.m32 * m.m22 + this.m33 * m.m32;
        double m33 = this.m30 * m.m03 + this.m31 * m.m13 + this.m32 * m.m23 + this.m33 * m.m33;
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;
        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;
        return this;
    }

    public Mat4 mul(Quaternionf q) {
        return this.mul(new Mat4(q));
    }

    public Mat4 translate(double x, double y, double z) {
        return this.mul(createTranslation(x, y, z));
    }

    public Mat4 translate(Vec3 v) {
        return this.translate(v.x, v.y, v.z);
    }

    public Mat4 scale(double x, double y, double z) {
        return this.mul(createScale(x, y, z));
    }

    public Mat4 scale(Vec3 v) {
        return this.scale(v.x, v.y, v.z);
    }

    public Mat4 rotate(Quaternionf q) {
        return this.mul(q);
    }

    public Mat4 rotateRad(Vector3f axis, float angle) {
        return this.mul(new Quaternionf().rotationAxis((float) (angle % (2 * Math.PI)), axis));
    }

    public Mat4 rotateDeg(Vector3f axis, float angle) {
        return this.mul(new Quaternionf().rotationAxis((float) Math.toRadians(angle % 360), axis));
    }

    public static Mat4 createTranslation(double x, double y, double z) {
        Mat4 m = Mat4.identity();
        m.m03 = x;
        m.m13 = y;
        m.m23 = z;
        return m;
    }

    public static Mat4 createScale(double x, double y, double z) {
        Mat4 m = Mat4.identity();
        m.m00 = x;
        m.m11 = y;
        m.m22 = z;
        return m;
    }

    public Matrix4f to4f() {
        return new Matrix4f(
                (float) m00,
                (float) m10,
                (float) m20,
                (float) m30,
                (float) m01,
                (float) m11,
                (float) m21,
                (float) m31,
                (float) m02,
                (float) m12,
                (float) m22,
                (float) m32,
                (float) m03,
                (float) m13,
                (float) m23,
                (float) m33);
    }

    public FloatBuffer toBuffer() {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        this.store(buffer);
        return buffer;
    }
}
