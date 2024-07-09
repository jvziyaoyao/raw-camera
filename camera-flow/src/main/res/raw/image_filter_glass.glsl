// 朦胧
void imageFilter(inout vec4 color) {
    float factor = 0.5;
    color.rgb = vec3(
    pow(color.r, factor),
    pow(color.g, factor),
    pow(color.b, factor)
    );
}