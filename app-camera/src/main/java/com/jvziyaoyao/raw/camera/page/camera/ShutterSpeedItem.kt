package com.jvziyaoyao.raw.camera.page.camera

/**
 * 1/8000
 * 1/6000
 * 1/5000
 * 1/4000
 * 1/3200
 * 1/2500
 * 1/2000
 * 1/1600
 * 1/1300
 * 1/1000
 * 1/800
 * 1/600
 * 1/500
 * 1/400
 * 1/300
 * 1/250
 * 1/200
 * 1/160
 * 1/125
 * 1/100
 * 1/80
 * 1/60
 * 1/50
 * 1/40
 * 1/30
 * 1/25
 * 1/20
 * 1/15
 * 1/12
 * 1/10
 * 1/8
 * 1/6
 * 1/5
 * 1/4
 * 0.3
 * 0.4
 * 0.5
 * 0.6
 * 0.8
 * 1
 * 1.2
 * 1.6
 * 2
 * 2.5
 * 3
 * 4
 * 5
 * 6
 * 8
 * 10
 * 13
 * 15
 * 20
 */

const val ONE_SECOND = 1000 * 1000 * 1000

enum class ShutterSpeedItem(
    val label: String,
    val time: Long,
) {
    E_1_8000(
        label = "1/8000",
        time = ONE_SECOND.div(8000).toLong(),
    ),
    E_1_6000(
        label = "1/6000",
        time = ONE_SECOND.div(6000).toLong(),
    ),
    E_1_5000(
        label = "1/5000",
        time = ONE_SECOND.div(5000).toLong(),
    ),
    E_1_4000(
        label = "1/4000",
        time = ONE_SECOND.div(4000).toLong(),
    ),
    E_1_3200(
        label = "1/3200",
        time = ONE_SECOND.div(3200).toLong(),
    ),
    E_1_2500(
        label = "1/2500",
        time = ONE_SECOND.div(2500).toLong(),
    ),
    E_1_2000(
        label = "1/2000",
        time = ONE_SECOND.div(2000).toLong(),
    ),
    E_1_1600(
        label = "1/1600",
        time = ONE_SECOND.div(1600).toLong(),
    ),
    E_1_1300(
        label = "1/1300",
        time = ONE_SECOND.div(1300).toLong(),
    ),
    E_1_1000(
        label = "1/1000",
        time = ONE_SECOND.div(1000).toLong(),
    ),
    E_1_800(
        label = "1/800",
        time = ONE_SECOND.div(800).toLong(),
    ),
    E_1_600(
        label = "1/600",
        time = ONE_SECOND.div(600).toLong(),
    ),
    E_1_500(
        label = "1/500",
        time = ONE_SECOND.div(500).toLong(),
    ),
    E_1_400(
        label = "1/400",
        time = ONE_SECOND.div(400).toLong(),
    ),
    E_1_300(
        label = "1/300",
        time = ONE_SECOND.div(300).toLong(),
    ),
    E_1_250(
        label = "1/250",
        time = ONE_SECOND.div(250).toLong(),
    ),
    E_1_200(
        label = "1/200",
        time = ONE_SECOND.div(200).toLong(),
    ),
    E_1_160(
        label = "1/160",
        time = ONE_SECOND.div(160).toLong(),
    ),
    E_1_125(
        label = "1/125",
        time = ONE_SECOND.div(125).toLong(),
    ),
    E_1_100(
        label = "1/100",
        time = ONE_SECOND.div(100).toLong(),
    ),
    E_1_80(
        label = "1/80",
        time = ONE_SECOND.div(80).toLong(),
    ),
    E_1_60(
        label = "1/60",
        time = ONE_SECOND.div(60).toLong(),
    ),
    E_1_50(
        label = "1/50",
        time = ONE_SECOND.div(50).toLong(),
    ),
    E_1_40(
        label = "1/40",
        time = ONE_SECOND.div(40).toLong(),
    ),
    E_1_30(
        label = "1/30",
        time = ONE_SECOND.div(30).toLong(),
    ),
    E_1_25(
        label = "1/25",
        time = ONE_SECOND.div(25).toLong(),
    ),
    E_1_20(
        label = "1/20",
        time = ONE_SECOND.div(20).toLong(),
    ),
    E_1_15(
        label = "1/15",
        time = ONE_SECOND.div(15).toLong(),
    ),
    E_1_12(
        label = "1/12",
        time = ONE_SECOND.div(12).toLong(),
    ),
    E_1_10(
        label = "1/10",
        time = ONE_SECOND.div(10).toLong(),
    ),
    E_1_8(
        label = "1/8",
        time = ONE_SECOND.div(8).toLong(),
    ),
    E_1_6(
        label = "1/6",
        time = ONE_SECOND.div(6).toLong(),
    ),
    E_1_5(
        label = "1/5",
        time = ONE_SECOND.div(5).toLong(),
    ),
    E_1_4(
        label = "1/4",
        time = ONE_SECOND.div(4).toLong(),
    ),
    E_0_3(
        label = "0.3",
        time = ONE_SECOND.times(0.3).toLong(),
    ),
    E_0_4(
        label = "0.4",
        time = ONE_SECOND.times(0.4).toLong(),
    ),
    E_0_5(
        label = "0.5",
        time = ONE_SECOND.times(0.5).toLong(),
    ),
    E_0_6(
        label = "0.6",
        time = ONE_SECOND.times(0.6).toLong(),
    ),
    E_0_8(
        label = "0.8",
        time = ONE_SECOND.times(0.8).toLong(),
    ),
    E_1(
        label = "1",
        time = ONE_SECOND.toLong(),
    ),
    E_1_D_2(
        label = "1.2",
        time = ONE_SECOND.times(1.2).toLong(),
    ),
    E_1_D_6(
        label = "1.6",
        time = ONE_SECOND.times(1.6).toLong(),
    ),
    E_2(
        label = "2",
        time = ONE_SECOND.times(2).toLong(),
    ),
    E_2_D_5(
        label = "2.5",
        time = ONE_SECOND.times(2.5).toLong(),
    ),
    E_3(
        label = "3",
        time = ONE_SECOND.times(3).toLong(),
    ),
    E_4(
        label = "4",
        time = ONE_SECOND.times(4).toLong(),
    ),
    E_5(
        label = "5",
        time = ONE_SECOND.times(5).toLong(),
    ),
    E_6(
        label = "6",
        time = ONE_SECOND.times(6).toLong(),
    ),
    E_8(
        label = "8",
        time = ONE_SECOND.times(8).toLong(),
    ),
    E_10(
        label = "10",
        time = ONE_SECOND.times(10).toLong(),
    ),
    E_12(
        label = "12",
        time = ONE_SECOND.times(12).toLong(),
    ),
    E_16(
        label = "16",
        time = ONE_SECOND.times(16).toLong(),
    ),
    E_20(
        label = "20",
        time = ONE_SECOND.times(20).toLong(),
    ),
    ;
}