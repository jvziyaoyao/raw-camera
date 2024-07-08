// 复古滤镜
void imageFilter(inout vec4 color){
    color.r = color.r * 0.9;
    color.g = color.g * 0.7;
    color.b = color.b * 0.4;
}