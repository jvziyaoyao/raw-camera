#version 300 es
precision mediump float;
uniform sampler2D uTextureUnit;
in vec2 vTexCoord;
out vec4 vFragColor;

//灰度滤镜
void grey(inout vec4 color){
    float weightMean = color.r * 0.3 + color.g * 0.59 + color.b * 0.11;
    color.r = color.g = color.b = weightMean;
}

void main() {
    vFragColor = texture(uTextureUnit, vTexCoord);
    grey(vFragColor);
}