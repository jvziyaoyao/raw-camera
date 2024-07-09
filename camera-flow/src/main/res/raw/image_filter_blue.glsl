// 蓝调滤镜
void imageFilter(inout vec4 color) {
    color.b += 0.2;
    color.b = clamp(color.b, 0.0, 1.0);
}