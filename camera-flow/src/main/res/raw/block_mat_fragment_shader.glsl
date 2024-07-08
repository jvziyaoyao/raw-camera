#version 300 es
precision mediump float;
uniform sampler2D uTextureUnit;
in vec2 vTexCoord;
out vec4 vFragColor;

void imageFilter(inout vec4 color) {}

void main() {
    vFragColor = texture(uTextureUnit, vTexCoord);
    imageFilter(vFragColor);
}