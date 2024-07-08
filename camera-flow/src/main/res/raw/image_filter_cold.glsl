// 冷色调
void imageFilter(inout vec4 color) {
    color.b += 0.1;
    color.g += 0.1;
    color.b = clamp(color.b, 0.0, 1.0);
    color.g = clamp(color.g, 0.0, 1.0);
}