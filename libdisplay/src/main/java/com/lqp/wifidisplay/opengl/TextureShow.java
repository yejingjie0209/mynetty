package com.lqp.wifidisplay.opengl;

import android.opengl.*;

import java.nio.*;

public class TextureShow {

    private final String vertexShaderCode =
            "#extension GL_OES_EGL_image_external : require\n"+
                    "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 position;" +
                    "attribute vec4 inputTextureCoordinate;" +
                    "varying vec2 textureCoordinate;" +
                    "void main()" +
                    "{"+
                    "gl_Position = uMVPMatrix * position;"+
                    "textureCoordinate = inputTextureCoordinate.xy;" +
                    "}";

    private final String fragmentShaderCode =
                "#extension GL_OES_EGL_image_external : require\n"+
                    "precision mediump float;" +
                    "varying vec2 textureCoordinate;                            \n" +
                    "uniform samplerExternalOES s_texture;               \n" +
                    "void main() {" +
                    "  gl_FragColor = texture2D( s_texture, textureCoordinate );\n" +
                    "}";

    private FloatBuffer vertexBuffer, textureVerticesBuffer;
    private ShortBuffer vertexIndexBuffer;
    private int matrixHandle;
    private final int mProgram;
    private int mPositionHandle;
    private int mTextureHandle;
    private int mTextureCoordHandle;


    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 2;
    static float squareVertices[] = { // in counterclockwise order:
            -1.0f,  1.0f,
            -1.0f,  -1.0f,
            1.0f,  -1.0f,
            1.0f,  1.0f
    };

    private short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

    static float textureVertices[] = { // in counterclockwise order:
             0f,  1.0f,
             0.0f,  0.0f,
             1.0f,  0.0f,
             1.0f,  1.0f
    };

    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex


    TextureShow() {
        ByteBuffer bb = ByteBuffer.allocateDirect(squareVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareVertices);
        vertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        vertexIndexBuffer = dlb.asShortBuffer();
        vertexIndexBuffer.put(drawOrder);
        vertexIndexBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(textureVertices.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        textureVerticesBuffer = bb2.asFloatBuffer();
        textureVerticesBuffer.put(textureVertices);
        textureVerticesBuffer.position(0);

        int vertexShader = GlUtil.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = GlUtil.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        GlUtil.checkLocation(mPositionHandle, "mPositionHandle");

        mTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        GlUtil.checkLocation(mTextureCoordHandle, "mTextureCoordHandle");

        mTextureHandle = GLES20.glGetUniformLocation(mProgram, "s_texture");
        GlUtil.checkLocation(mTextureHandle, "mTextureHandle");

        matrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GlUtil.checkLocation(matrixHandle, "matrixHandle");
    }

    public void onDrawFrame(float[] matrix, int textureId) {
        GLES20.glUseProgram(mProgram);


        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GlUtil.checkGlError("glActiveTexture");
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GlUtil.checkGlError("glBindTexture");

        GLES20.glUniform1i(mTextureHandle, 0);
        GlUtil.checkGlError("glUniform1i");

        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, matrix, 0);

        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,vertexStride, vertexBuffer);
        GlUtil.checkGlError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,vertexStride, textureVerticesBuffer);
        GlUtil.checkGlError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, vertexIndexBuffer);
        GlUtil.checkGlError("glDrawElements");
    }
}