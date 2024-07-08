void imageFilter(inout vec4 color) {
    float contrast = 1.5;
    color.rgb = (color.rgb - 0.5) * contrast + 0.5;
}
