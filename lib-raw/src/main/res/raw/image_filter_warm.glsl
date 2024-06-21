// 暖色调
void imageFilter(inout vec4 color) {
    color.r += 0.1;
    color.g += 0.1;
    color.r = clamp(color.r, 0.0, 1.0);
    color.g = clamp(color.g, 0.0, 1.0);
}