#version 300 es
//OpenGL ES3.0外部纹理扩展
//#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;
uniform sampler2D yTexture;
uniform sampler2D uTexture;
uniform sampler2D vTexture;
uniform sampler2D additionalTexture;
uniform bool useAdditionalTexture;
in vec2 vTexCoord;
out vec4 vFragColor;
void main() {
    //    vec4 cameraColor = texture(cameraTexture, vTexCoord);
    //    vec4 additionalColor = texture(additionalTexture, vTexCoord);
    //    float r = additionalColor.r + (1.0 - additionalColor.a) * cameraColor.r;
    //    float g = additionalColor.g + (1.0 - additionalColor.a) * cameraColor.g;
    //    float b = additionalColor.b + (1.0 - additionalColor.a) * cameraColor.b;
    //    vFragColor = vec4(r, g, b, 1.0);

//            vFragColor = texture(yTexture, vTexCoord);
//        vFragColor = texture(uTexture, vTexCoord);
//        vFragColor = texture(vTexture, vTexCoord);

    float y, u, v;
    y = texture(yTexture, vTexCoord).r;
    u = texture(uTexture, vTexCoord).r- 0.5;
    v = texture(vTexture, vTexCoord).r- 0.5;
    vec3 rgb;
    rgb.r = y + 1.403 * v;
    rgb.g = y - 0.344 * u - 0.714 * v;
    rgb.b = y + 1.770 * u;
    vec4 cameraColor = vec4(rgb, 1);

    if (useAdditionalTexture) {
        vec4 additionalColor = texture(additionalTexture, vTexCoord);
        float r = additionalColor.r + (1.0 - additionalColor.a) * cameraColor.r;
        float g = additionalColor.g + (1.0 - additionalColor.a) * cameraColor.g;
        float b = additionalColor.b + (1.0 - additionalColor.a) * cameraColor.b;
        vFragColor = vec4(r, g, b, 1.0);
    } else {
        vFragColor = cameraColor;
    }
}