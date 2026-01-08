# TDS_SuplaDeviceChannel_.DefaultIcon (supla-cloud)

`DefaultIcon` maps to the channel's alternative icon index (`altIcon`) in
supla-cloud. The cloud enforces valid values by comparing `altIcon` against
`ChannelFunction::getMaxAlternativeIconIndex()`.

That means valid values are:
- Always: `0` (default icon).
- For functions listed below: any integer in `1..max` for that function.

Source of limits: `supla-cloud/src/SuplaBundle/Enums/ChannelFunction.php`
(`maxAlternativeIconIndexes()`), enforced by
`supla-cloud/src/SuplaBundle/Entity/Main/IODeviceChannel.php::setAltIcon()`.

## Allowed ranges by function
- POWERSWITCH: 0..7
- LIGHTSWITCH: 0..2
- CONTROLLINGTHEGATE: 0..2
- OPENINGSENSOR_GATE: 0..2
- STAIRCASETIMER: 0..1
- THERMOSTAT: 0..3
- THERMOSTATHEATPOLHOMEPLUS: 0..3
- DIGIGLASS_VERTICAL: 0..1
- DIGIGLASS_HORIZONTAL: 0..1
- ELECTRICITYMETER: 0..1
- SCENE: 0..19
- GENERAL_PURPOSE_MEASUREMENT: 0..45
- GENERAL_PURPOSE_METER: 0..14
- THERMOMETER: 0..7
- HUMIDITYANDTEMPERATURE: 0..7
- ALARM_ARMAMENT_SENSOR: 0..3
- HEATORCOLDSOURCESWITCH: 0..5
- CONTAINER: 0..3
- SEPTIC_TANK: 0..1
- WATER_TANK: 0..3
- MOTION_SENSOR: 0..4
- BINARY_SENSOR: 0..9
- ACTION_TRIGGER: 0..45

Functions not listed above only allow `0`.

## Icon filenames

### Function icons (supla-cloud/web/assets/img/functions)

```text
0.svg
10.svg
100.svg
1000.svg
1000-closed.svg
100-closed.svg
1010.svg
1010_1.svg
1010_1-closed.svg
1010_2.svg
1010_2-closed.svg
1010_3.svg
1010_3-closed.svg
1010_4.svg
1010_4-closed.svg
1010-closed.svg
1020.svg
1020_1.svg
1020_1-closed.svg
1020_2.svg
1020_2-closed.svg
1020_3.svg
1020_3-closed.svg
1020_4.svg
1020_4-closed.svg
1020_5.svg
1020_5-closed.svg
1020_6.svg
1020_6-closed.svg
1020_7.svg
1020_7-closed.svg
1020_8.svg
1020_8-closed.svg
1020_9.svg
1020_9-closed.svg
1020-closed.svg
10-closed.svg
110.svg
110-closed.svg
115.svg
115-closed.svg
120.svg
120-closed.svg
125.svg
125-closed.svg
130.svg
130_1.svg
130_1-on.svg
130_2.svg
130_2-on.svg
130_3.svg
130_3-on.svg
130_4.svg
130_4-on.svg
130_5.svg
130_5-on.svg
130_6.svg
130_6-on.svg
130_7.svg
130_7-on.svg
130-on.svg
140.svg
140_1.svg
140_1-on.svg
140_2.svg
140_2-on.svg
140-on.svg
180.svg
180-on.svg
190.svg
190-on.svg
20.svg
20_1.svg
20_1-closed.svg
20_1-partial.svg
20_2.svg
20_2-closed.svg
20_2-partial.svg
200.svg
2000.svg
2000_1.svg
2000_10.svg
2000_11.svg
2000_12.svg
2000_13.svg
2000_14.svg
2000_15.svg
2000_16.svg
2000_17.svg
2000_18.svg
2000_19.svg
2000_2.svg
2000_3.svg
2000_4.svg
2000_5.svg
2000_6.svg
2000_7.svg
2000_8.svg
2000_9.svg
200-offoff.svg
200-offon.svg
200-onoff.svg
200-onon.svg
2010.svg
20-closed.svg
20-partial.svg
210.svg
220.svg
230.svg
230-closed.svg
235.svg
235-closed.svg
236.svg
236_1.svg
236_1-closed.svg
236_2.svg
236_2-closed.svg
236_3.svg
236_3-closed.svg
236-closed.svg
240.svg
240-closed.svg
250.svg
260.svg
270.svg
280.svg
290.svg
30.svg
300.svg
300_1.svg
300_1-on.svg
300-on.svg
30-closed.svg
30-partial.svg
310.svg
310_1.svg
315.svg
320.svg
330.svg
340.svg
40.svg
40_1.svg
40_2.svg
40_3.svg
40_4.svg
40_5.svg
40_6.svg
40_7.svg
40_b.svg
400.svg
400_1.svg
400_1-on.svg
400_2.svg
400_2-on.svg
400_3.svg
400_3-on.svg
400-on.svg
410.svg
410_1.svg
410_1-on.svg
410_2.svg
410_2-on.svg
410_3.svg
410_3-on.svg
410-on.svg
42.svg
420.svg
420-cool.svg
422.svg
423.svg
424.svg
425.svg
426.svg
45.svg
45_1.svg
45_1-hum.svg
45_2.svg
45_2-hum.svg
45_3.svg
45_3-hum.svg
45_4.svg
45_4-hum.svg
45_5.svg
45_5-hum.svg
45_6.svg
45_6-hum.svg
45_7.svg
45_7-hum.svg
45-hum.svg
50.svg
500.svg
500-closed.svg
50-closed.svg
510.svg
510-closed.svg
520.svg
520_1.svg
520_10.svg
520_11.svg
520_12.svg
520_13.svg
520_14.svg
520_15.svg
520_16.svg
520_17.svg
520_18.svg
520_19.svg
520_2.svg
520_20.svg
520_21.svg
520_22.svg
520_23.svg
520_24.svg
520_25.svg
520_26.svg
520_27.svg
520_28.svg
520_29.svg
520_3.svg
520_30.svg
520_31.svg
520_32.svg
520_33.svg
520_34.svg
520_35.svg
520_36.svg
520_37.svg
520_38.svg
520_39.svg
520_4.svg
520_40.svg
520_41.svg
520_42.svg
520_43.svg
520_44.svg
520_45.svg
520_5.svg
520_6.svg
520_7.svg
520_8.svg
520_9.svg
530.svg
530_1.svg
530_10.svg
530_11.svg
530_12.svg
530_13.svg
530_14.svg
530_2.svg
530_3.svg
530_4.svg
530_5.svg
530_6.svg
530_7.svg
530_8.svg
530_9.svg
60.svg
60_1.svg
60_1-closed.svg
60_2.svg
60_2-closed.svg
60-closed.svg
70.svg
700.svg
700_1.svg
700_10.svg
700_11.svg
700_12.svg
700_13.svg
700_14.svg
700_15.svg
700_16.svg
700_17.svg
700_18.svg
700_19.svg
700_2.svg
700_20.svg
700_21.svg
700_22.svg
700_23.svg
700_24.svg
700_25.svg
700_26.svg
700_27.svg
700_28.svg
700_29.svg
700_3.svg
700_30.svg
700_31.svg
700_32.svg
700_33.svg
700_34.svg
700_35.svg
700_36.svg
700_37.svg
700_38.svg
700_39.svg
700_4.svg
700_40.svg
700_41.svg
700_42.svg
700_43.svg
700_44.svg
700_45.svg
700_5.svg
700_6.svg
700_7.svg
700_8.svg
700_9.svg
70-closed.svg
80.svg
800.svg
800_1.svg
800_1-transparent.svg
800-transparent.svg
80-closed.svg
810.svg
810_1.svg
810_1-transparent.svg
810-transparent.svg
90.svg
900.svg
900-closed.svg
90-closed.svg
910.svg
910-closed.svg
920.svg
920-closed.svg
930.svg
930-closed.svg
940.svg
940-closed.svg
950.svg
950-closed.svg
960.svg
960-on.svg
970.svg
970_1.svg
970_1-on.svg
970_2.svg
970_2-on.svg
970_3.svg
970_3-on.svg
970_4.svg
970_4-on.svg
970_5.svg
970_5-on.svg
970-on.svg
980.svg
980_1.svg
980_1-full.svg
980_1-half.svg
980_2.svg
980_2-full.svg
980_2-half.svg
980_3.svg
980_3-full.svg
980_3-half.svg
980-full.svg
980-half.svg
981.svg
981_1.svg
981_1-full.svg
981_1-half.svg
981-full.svg
981-half.svg
982.svg
982_1.svg
982_1-full.svg
982_1-half.svg
982_2.svg
982_2-full.svg
982_2-half.svg
982_3.svg
982_3-full.svg
982_3-half.svg
982-full.svg
982-half.svg
990.svg
990-closed.svg
curtains1.svg
curtains2.svg
curtains3.svg
curtains4.svg
curtains5.svg
sensor.svg
```

### Frontend asset icons (supla-cloud/src/frontend/src/assets/icons)

```text
moon.svg
scene.svg
sun.svg
sunrise.svg
sunset.svg
```


## Icon descriptions

### Function icons

| Filename | Description |
| --- | --- |
| 0.svg | NONE (id 0) icon |
| 10.svg | CONTROLLINGTHEGATEWAYLOCK (id 10) icon |
| 100.svg | OPENINGSENSOR_DOOR (id 100) icon |
| 1000.svg | FLOOD_SENSOR (id 1000) icon |
| 1000-closed.svg | FLOOD_SENSOR (id 1000) icon; state: closed |
| 100-closed.svg | OPENINGSENSOR_DOOR (id 100) icon; state: closed |
| 1010.svg | MOTION_SENSOR (id 1010) icon |
| 1010_1.svg | MOTION_SENSOR (id 1010) icon; alternative icon index 1 |
| 1010_1-closed.svg | MOTION_SENSOR (id 1010) icon; alternative icon index 1; state: closed |
| 1010_2.svg | MOTION_SENSOR (id 1010) icon; alternative icon index 2 |
| 1010_2-closed.svg | MOTION_SENSOR (id 1010) icon; alternative icon index 2; state: closed |
| 1010_3.svg | MOTION_SENSOR (id 1010) icon; alternative icon index 3 |
| 1010_3-closed.svg | MOTION_SENSOR (id 1010) icon; alternative icon index 3; state: closed |
| 1010_4.svg | MOTION_SENSOR (id 1010) icon; alternative icon index 4 |
| 1010_4-closed.svg | MOTION_SENSOR (id 1010) icon; alternative icon index 4; state: closed |
| 1010-closed.svg | MOTION_SENSOR (id 1010) icon; state: closed |
| 1020.svg | BINARY_SENSOR (id 1020) icon |
| 1020_1.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 1 |
| 1020_1-closed.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 1; state: closed |
| 1020_2.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 2 |
| 1020_2-closed.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 2; state: closed |
| 1020_3.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 3 |
| 1020_3-closed.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 3; state: closed |
| 1020_4.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 4 |
| 1020_4-closed.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 4; state: closed |
| 1020_5.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 5 |
| 1020_5-closed.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 5; state: closed |
| 1020_6.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 6 |
| 1020_6-closed.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 6; state: closed |
| 1020_7.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 7 |
| 1020_7-closed.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 7; state: closed |
| 1020_8.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 8 |
| 1020_8-closed.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 8; state: closed |
| 1020_9.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 9 |
| 1020_9-closed.svg | BINARY_SENSOR (id 1020) icon; alternative icon index 9; state: closed |
| 1020-closed.svg | BINARY_SENSOR (id 1020) icon; state: closed |
| 10-closed.svg | CONTROLLINGTHEGATEWAYLOCK (id 10) icon; state: closed |
| 110.svg | CONTROLLINGTHEROLLERSHUTTER (id 110) icon |
| 110-closed.svg | CONTROLLINGTHEROLLERSHUTTER (id 110) icon; state: closed |
| 115.svg | CONTROLLINGTHEROOFWINDOW (id 115) icon |
| 115-closed.svg | CONTROLLINGTHEROOFWINDOW (id 115) icon; state: closed |
| 120.svg | OPENINGSENSOR_ROLLERSHUTTER (id 120) icon |
| 120-closed.svg | OPENINGSENSOR_ROLLERSHUTTER (id 120) icon; state: closed |
| 125.svg | OPENINGSENSOR_ROOFWINDOW (id 125) icon |
| 125-closed.svg | OPENINGSENSOR_ROOFWINDOW (id 125) icon; state: closed |
| 130.svg | POWERSWITCH (id 130) icon |
| 130_1.svg | POWERSWITCH (id 130) icon; alternative icon index 1 |
| 130_1-on.svg | POWERSWITCH (id 130) icon; alternative icon index 1; state: on |
| 130_2.svg | POWERSWITCH (id 130) icon; alternative icon index 2 |
| 130_2-on.svg | POWERSWITCH (id 130) icon; alternative icon index 2; state: on |
| 130_3.svg | POWERSWITCH (id 130) icon; alternative icon index 3 |
| 130_3-on.svg | POWERSWITCH (id 130) icon; alternative icon index 3; state: on |
| 130_4.svg | POWERSWITCH (id 130) icon; alternative icon index 4 |
| 130_4-on.svg | POWERSWITCH (id 130) icon; alternative icon index 4; state: on |
| 130_5.svg | POWERSWITCH (id 130) icon; alternative icon index 5 |
| 130_5-on.svg | POWERSWITCH (id 130) icon; alternative icon index 5; state: on |
| 130_6.svg | POWERSWITCH (id 130) icon; alternative icon index 6 |
| 130_6-on.svg | POWERSWITCH (id 130) icon; alternative icon index 6; state: on |
| 130_7.svg | POWERSWITCH (id 130) icon; alternative icon index 7 |
| 130_7-on.svg | POWERSWITCH (id 130) icon; alternative icon index 7; state: on |
| 130-on.svg | POWERSWITCH (id 130) icon; state: on |
| 140.svg | LIGHTSWITCH (id 140) icon |
| 140_1.svg | LIGHTSWITCH (id 140) icon; alternative icon index 1 |
| 140_1-on.svg | LIGHTSWITCH (id 140) icon; alternative icon index 1; state: on |
| 140_2.svg | LIGHTSWITCH (id 140) icon; alternative icon index 2 |
| 140_2-on.svg | LIGHTSWITCH (id 140) icon; alternative icon index 2; state: on |
| 140-on.svg | LIGHTSWITCH (id 140) icon; state: on |
| 180.svg | DIMMER (id 180) icon |
| 180-on.svg | DIMMER (id 180) icon; state: on |
| 190.svg | RGBLIGHTING (id 190) icon |
| 190-on.svg | RGBLIGHTING (id 190) icon; state: on |
| 20.svg | CONTROLLINGTHEGATE (id 20) icon |
| 20_1.svg | CONTROLLINGTHEGATE (id 20) icon; alternative icon index 1 |
| 20_1-closed.svg | CONTROLLINGTHEGATE (id 20) icon; alternative icon index 1; state: closed |
| 20_1-partial.svg | CONTROLLINGTHEGATE (id 20) icon; alternative icon index 1; state: partially closed |
| 20_2.svg | CONTROLLINGTHEGATE (id 20) icon; alternative icon index 2 |
| 20_2-closed.svg | CONTROLLINGTHEGATE (id 20) icon; alternative icon index 2; state: closed |
| 20_2-partial.svg | CONTROLLINGTHEGATE (id 20) icon; alternative icon index 2; state: partially closed |
| 200.svg | DIMMERANDRGBLIGHTING (id 200) icon |
| 2000.svg | SCENE (id 2000) icon |
| 2000_1.svg | SCENE (id 2000) icon; alternative icon index 1 |
| 2000_10.svg | SCENE (id 2000) icon; alternative icon index 10 |
| 2000_11.svg | SCENE (id 2000) icon; alternative icon index 11 |
| 2000_12.svg | SCENE (id 2000) icon; alternative icon index 12 |
| 2000_13.svg | SCENE (id 2000) icon; alternative icon index 13 |
| 2000_14.svg | SCENE (id 2000) icon; alternative icon index 14 |
| 2000_15.svg | SCENE (id 2000) icon; alternative icon index 15 |
| 2000_16.svg | SCENE (id 2000) icon; alternative icon index 16 |
| 2000_17.svg | SCENE (id 2000) icon; alternative icon index 17 |
| 2000_18.svg | SCENE (id 2000) icon; alternative icon index 18 |
| 2000_19.svg | SCENE (id 2000) icon; alternative icon index 19 |
| 2000_2.svg | SCENE (id 2000) icon; alternative icon index 2 |
| 2000_3.svg | SCENE (id 2000) icon; alternative icon index 3 |
| 2000_4.svg | SCENE (id 2000) icon; alternative icon index 4 |
| 2000_5.svg | SCENE (id 2000) icon; alternative icon index 5 |
| 2000_6.svg | SCENE (id 2000) icon; alternative icon index 6 |
| 2000_7.svg | SCENE (id 2000) icon; alternative icon index 7 |
| 2000_8.svg | SCENE (id 2000) icon; alternative icon index 8 |
| 2000_9.svg | SCENE (id 2000) icon; alternative icon index 9 |
| 200-offoff.svg | DIMMERANDRGBLIGHTING (id 200) icon; RGB/dimmer state: dim off, color off |
| 200-offon.svg | DIMMERANDRGBLIGHTING (id 200) icon; RGB/dimmer state: dim off, color on |
| 200-onoff.svg | DIMMERANDRGBLIGHTING (id 200) icon; RGB/dimmer state: dim on, color off |
| 200-onon.svg | DIMMERANDRGBLIGHTING (id 200) icon; RGB/dimmer state: dim on, color on |
| 2010.svg | SCHEDULE (id 2010) icon |
| 20-closed.svg | CONTROLLINGTHEGATE (id 20) icon; state: closed |
| 20-partial.svg | CONTROLLINGTHEGATE (id 20) icon; state: partially closed |
| 210.svg | DEPTHSENSOR (id 210) icon |
| 220.svg | DISTANCESENSOR (id 220) icon |
| 230.svg | OPENINGSENSOR_WINDOW (id 230) icon |
| 230-closed.svg | OPENINGSENSOR_WINDOW (id 230) icon; state: closed |
| 235.svg | HOTELCARDSENSOR (id 235) icon |
| 235-closed.svg | HOTELCARDSENSOR (id 235) icon; state: closed |
| 236.svg | ALARM_ARMAMENT_SENSOR (id 236) icon |
| 236_1.svg | ALARM_ARMAMENT_SENSOR (id 236) icon; alternative icon index 1 |
| 236_1-closed.svg | ALARM_ARMAMENT_SENSOR (id 236) icon; alternative icon index 1; state: closed |
| 236_2.svg | ALARM_ARMAMENT_SENSOR (id 236) icon; alternative icon index 2 |
| 236_2-closed.svg | ALARM_ARMAMENT_SENSOR (id 236) icon; alternative icon index 2; state: closed |
| 236_3.svg | ALARM_ARMAMENT_SENSOR (id 236) icon; alternative icon index 3 |
| 236_3-closed.svg | ALARM_ARMAMENT_SENSOR (id 236) icon; alternative icon index 3; state: closed |
| 236-closed.svg | ALARM_ARMAMENT_SENSOR (id 236) icon; state: closed |
| 240.svg | MAILSENSOR (id 240) icon |
| 240-closed.svg | MAILSENSOR (id 240) icon; state: closed |
| 250.svg | WINDSENSOR (id 250) icon |
| 260.svg | PRESSURESENSOR (id 260) icon |
| 270.svg | RAINSENSOR (id 270) icon |
| 280.svg | WEIGHTSENSOR (id 280) icon |
| 290.svg | WEATHER_STATION (id 290) icon |
| 30.svg | CONTROLLINGTHEGARAGEDOOR (id 30) icon |
| 300.svg | STAIRCASETIMER (id 300) icon |
| 300_1.svg | STAIRCASETIMER (id 300) icon; alternative icon index 1 |
| 300_1-on.svg | STAIRCASETIMER (id 300) icon; alternative icon index 1; state: on |
| 300-on.svg | STAIRCASETIMER (id 300) icon; state: on |
| 30-closed.svg | CONTROLLINGTHEGARAGEDOOR (id 30) icon; state: closed |
| 30-partial.svg | CONTROLLINGTHEGARAGEDOOR (id 30) icon; state: partially closed |
| 310.svg | ELECTRICITYMETER (id 310) icon |
| 310_1.svg | ELECTRICITYMETER (id 310) icon; alternative icon index 1 |
| 315.svg | IC_ELECTRICITYMETER (id 315) icon |
| 320.svg | IC_GASMETER (id 320) icon |
| 330.svg | IC_WATERMETER (id 330) icon |
| 340.svg | IC_HEATMETER (id 340) icon |
| 40.svg | THERMOMETER (id 40) icon |
| 40_1.svg | THERMOMETER (id 40) icon; alternative icon index 1 |
| 40_2.svg | THERMOMETER (id 40) icon; alternative icon index 2 |
| 40_3.svg | THERMOMETER (id 40) icon; alternative icon index 3 |
| 40_4.svg | THERMOMETER (id 40) icon; alternative icon index 4 |
| 40_5.svg | THERMOMETER (id 40) icon; alternative icon index 5 |
| 40_6.svg | THERMOMETER (id 40) icon; alternative icon index 6 |
| 40_7.svg | THERMOMETER (id 40) icon; alternative icon index 7 |
| 40_b.svg | Named function icon '40_b' |
| 400.svg | THERMOSTAT (id 400) icon |
| 400_1.svg | THERMOSTAT (id 400) icon; alternative icon index 1 |
| 400_1-on.svg | THERMOSTAT (id 400) icon; alternative icon index 1; state: on |
| 400_2.svg | THERMOSTAT (id 400) icon; alternative icon index 2 |
| 400_2-on.svg | THERMOSTAT (id 400) icon; alternative icon index 2; state: on |
| 400_3.svg | THERMOSTAT (id 400) icon; alternative icon index 3 |
| 400_3-on.svg | THERMOSTAT (id 400) icon; alternative icon index 3; state: on |
| 400-on.svg | THERMOSTAT (id 400) icon; state: on |
| 410.svg | THERMOSTATHEATPOLHOMEPLUS (id 410) icon |
| 410_1.svg | THERMOSTATHEATPOLHOMEPLUS (id 410) icon; alternative icon index 1 |
| 410_1-on.svg | THERMOSTATHEATPOLHOMEPLUS (id 410) icon; alternative icon index 1; state: on |
| 410_2.svg | THERMOSTATHEATPOLHOMEPLUS (id 410) icon; alternative icon index 2 |
| 410_2-on.svg | THERMOSTATHEATPOLHOMEPLUS (id 410) icon; alternative icon index 2; state: on |
| 410_3.svg | THERMOSTATHEATPOLHOMEPLUS (id 410) icon; alternative icon index 3 |
| 410_3-on.svg | THERMOSTATHEATPOLHOMEPLUS (id 410) icon; alternative icon index 3; state: on |
| 410-on.svg | THERMOSTATHEATPOLHOMEPLUS (id 410) icon; state: on |
| 42.svg | HUMIDITY (id 42) icon |
| 420.svg | HVAC_THERMOSTAT (id 420) icon |
| 420-cool.svg | HVAC_THERMOSTAT (id 420) icon; HVAC cool variant |
| 422.svg | HVAC_THERMOSTAT_HEAT_COOL (id 422) icon |
| 423.svg | HVAC_DRYER (id 423) icon |
| 424.svg | HVAC_FAN (id 424) icon |
| 425.svg | HVAC_THERMOSTAT_DIFFERENTIAL (id 425) icon |
| 426.svg | HVAC_DOMESTIC_HOT_WATER (id 426) icon |
| 45.svg | HUMIDITYANDTEMPERATURE (id 45) icon |
| 45_1.svg | HUMIDITYANDTEMPERATURE (id 45) icon; alternative icon index 1 |
| 45_1-hum.svg | HUMIDITYANDTEMPERATURE (id 45) icon; alternative icon index 1; humidity variant for HUMIDITYANDTEMPERATURE dual icon |
| 45_2.svg | HUMIDITYANDTEMPERATURE (id 45) icon; alternative icon index 2 |
| 45_2-hum.svg | HUMIDITYANDTEMPERATURE (id 45) icon; alternative icon index 2; humidity variant for HUMIDITYANDTEMPERATURE dual icon |
| 45_3.svg | HUMIDITYANDTEMPERATURE (id 45) icon; alternative icon index 3 |
| 45_3-hum.svg | HUMIDITYANDTEMPERATURE (id 45) icon; alternative icon index 3; humidity variant for HUMIDITYANDTEMPERATURE dual icon |
| 45_4.svg | HUMIDITYANDTEMPERATURE (id 45) icon; alternative icon index 4 |
| 45_4-hum.svg | HUMIDITYANDTEMPERATURE (id 45) icon; alternative icon index 4; humidity variant for HUMIDITYANDTEMPERATURE dual icon |
| 45_5.svg | HUMIDITYANDTEMPERATURE (id 45) icon; alternative icon index 5 |
| 45_5-hum.svg | HUMIDITYANDTEMPERATURE (id 45) icon; alternative icon index 5; humidity variant for HUMIDITYANDTEMPERATURE dual icon |
| 45_6.svg | HUMIDITYANDTEMPERATURE (id 45) icon; alternative icon index 6 |
| 45_6-hum.svg | HUMIDITYANDTEMPERATURE (id 45) icon; alternative icon index 6; humidity variant for HUMIDITYANDTEMPERATURE dual icon |
| 45_7.svg | HUMIDITYANDTEMPERATURE (id 45) icon; alternative icon index 7 |
| 45_7-hum.svg | HUMIDITYANDTEMPERATURE (id 45) icon; alternative icon index 7; humidity variant for HUMIDITYANDTEMPERATURE dual icon |
| 45-hum.svg | HUMIDITYANDTEMPERATURE (id 45) icon; humidity variant for HUMIDITYANDTEMPERATURE dual icon |
| 50.svg | OPENINGSENSOR_GATEWAY (id 50) icon |
| 500.svg | VALVEOPENCLOSE (id 500) icon |
| 500-closed.svg | VALVEOPENCLOSE (id 500) icon; state: closed |
| 50-closed.svg | OPENINGSENSOR_GATEWAY (id 50) icon; state: closed |
| 510.svg | VALVEPERCENTAGE (id 510) icon |
| 510-closed.svg | VALVEPERCENTAGE (id 510) icon; state: closed |
| 520.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon |
| 520_1.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 1 |
| 520_10.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 10 |
| 520_11.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 11 |
| 520_12.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 12 |
| 520_13.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 13 |
| 520_14.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 14 |
| 520_15.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 15 |
| 520_16.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 16 |
| 520_17.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 17 |
| 520_18.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 18 |
| 520_19.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 19 |
| 520_2.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 2 |
| 520_20.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 20 |
| 520_21.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 21 |
| 520_22.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 22 |
| 520_23.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 23 |
| 520_24.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 24 |
| 520_25.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 25 |
| 520_26.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 26 |
| 520_27.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 27 |
| 520_28.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 28 |
| 520_29.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 29 |
| 520_3.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 3 |
| 520_30.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 30 |
| 520_31.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 31 |
| 520_32.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 32 |
| 520_33.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 33 |
| 520_34.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 34 |
| 520_35.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 35 |
| 520_36.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 36 |
| 520_37.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 37 |
| 520_38.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 38 |
| 520_39.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 39 |
| 520_4.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 4 |
| 520_40.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 40 |
| 520_41.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 41 |
| 520_42.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 42 |
| 520_43.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 43 |
| 520_44.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 44 |
| 520_45.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 45 |
| 520_5.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 5 |
| 520_6.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 6 |
| 520_7.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 7 |
| 520_8.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 8 |
| 520_9.svg | GENERAL_PURPOSE_MEASUREMENT (id 520) icon; alternative icon index 9 |
| 530.svg | GENERAL_PURPOSE_METER (id 530) icon |
| 530_1.svg | GENERAL_PURPOSE_METER (id 530) icon; alternative icon index 1 |
| 530_10.svg | GENERAL_PURPOSE_METER (id 530) icon; alternative icon index 10 |
| 530_11.svg | GENERAL_PURPOSE_METER (id 530) icon; alternative icon index 11 |
| 530_12.svg | GENERAL_PURPOSE_METER (id 530) icon; alternative icon index 12 |
| 530_13.svg | GENERAL_PURPOSE_METER (id 530) icon; alternative icon index 13 |
| 530_14.svg | GENERAL_PURPOSE_METER (id 530) icon; alternative icon index 14 |
| 530_2.svg | GENERAL_PURPOSE_METER (id 530) icon; alternative icon index 2 |
| 530_3.svg | GENERAL_PURPOSE_METER (id 530) icon; alternative icon index 3 |
| 530_4.svg | GENERAL_PURPOSE_METER (id 530) icon; alternative icon index 4 |
| 530_5.svg | GENERAL_PURPOSE_METER (id 530) icon; alternative icon index 5 |
| 530_6.svg | GENERAL_PURPOSE_METER (id 530) icon; alternative icon index 6 |
| 530_7.svg | GENERAL_PURPOSE_METER (id 530) icon; alternative icon index 7 |
| 530_8.svg | GENERAL_PURPOSE_METER (id 530) icon; alternative icon index 8 |
| 530_9.svg | GENERAL_PURPOSE_METER (id 530) icon; alternative icon index 9 |
| 60.svg | OPENINGSENSOR_GATE (id 60) icon |
| 60_1.svg | OPENINGSENSOR_GATE (id 60) icon; alternative icon index 1 |
| 60_1-closed.svg | OPENINGSENSOR_GATE (id 60) icon; alternative icon index 1; state: closed |
| 60_2.svg | OPENINGSENSOR_GATE (id 60) icon; alternative icon index 2 |
| 60_2-closed.svg | OPENINGSENSOR_GATE (id 60) icon; alternative icon index 2; state: closed |
| 60-closed.svg | OPENINGSENSOR_GATE (id 60) icon; state: closed |
| 70.svg | OPENINGSENSOR_GARAGEDOOR (id 70) icon |
| 700.svg | ACTION_TRIGGER (id 700) icon |
| 700_1.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 1 |
| 700_10.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 10 |
| 700_11.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 11 |
| 700_12.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 12 |
| 700_13.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 13 |
| 700_14.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 14 |
| 700_15.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 15 |
| 700_16.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 16 |
| 700_17.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 17 |
| 700_18.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 18 |
| 700_19.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 19 |
| 700_2.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 2 |
| 700_20.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 20 |
| 700_21.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 21 |
| 700_22.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 22 |
| 700_23.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 23 |
| 700_24.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 24 |
| 700_25.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 25 |
| 700_26.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 26 |
| 700_27.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 27 |
| 700_28.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 28 |
| 700_29.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 29 |
| 700_3.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 3 |
| 700_30.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 30 |
| 700_31.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 31 |
| 700_32.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 32 |
| 700_33.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 33 |
| 700_34.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 34 |
| 700_35.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 35 |
| 700_36.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 36 |
| 700_37.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 37 |
| 700_38.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 38 |
| 700_39.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 39 |
| 700_4.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 4 |
| 700_40.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 40 |
| 700_41.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 41 |
| 700_42.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 42 |
| 700_43.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 43 |
| 700_44.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 44 |
| 700_45.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 45 |
| 700_5.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 5 |
| 700_6.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 6 |
| 700_7.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 7 |
| 700_8.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 8 |
| 700_9.svg | ACTION_TRIGGER (id 700) icon; alternative icon index 9 |
| 70-closed.svg | OPENINGSENSOR_GARAGEDOOR (id 70) icon; state: closed |
| 80.svg | NOLIQUIDSENSOR (id 80) icon |
| 800.svg | DIGIGLASS_HORIZONTAL (id 800) icon |
| 800_1.svg | DIGIGLASS_HORIZONTAL (id 800) icon; alternative icon index 1 |
| 800_1-transparent.svg | DIGIGLASS_HORIZONTAL (id 800) icon; alternative icon index 1; state: transparent |
| 800-transparent.svg | DIGIGLASS_HORIZONTAL (id 800) icon; state: transparent |
| 80-closed.svg | NOLIQUIDSENSOR (id 80) icon; state: closed |
| 810.svg | DIGIGLASS_VERTICAL (id 810) icon |
| 810_1.svg | DIGIGLASS_VERTICAL (id 810) icon; alternative icon index 1 |
| 810_1-transparent.svg | DIGIGLASS_VERTICAL (id 810) icon; alternative icon index 1; state: transparent |
| 810-transparent.svg | DIGIGLASS_VERTICAL (id 810) icon; state: transparent |
| 90.svg | CONTROLLINGTHEDOORLOCK (id 90) icon |
| 900.svg | CONTROLLINGTHEFACADEBLIND (id 900) icon |
| 900-closed.svg | CONTROLLINGTHEFACADEBLIND (id 900) icon; state: closed |
| 90-closed.svg | CONTROLLINGTHEDOORLOCK (id 90) icon; state: closed |
| 910.svg | TERRACE_AWNING (id 910) icon |
| 910-closed.svg | TERRACE_AWNING (id 910) icon; state: closed |
| 920.svg | PROJECTOR_SCREEN (id 920) icon |
| 920-closed.svg | PROJECTOR_SCREEN (id 920) icon; state: closed |
| 930.svg | CURTAIN (id 930) icon |
| 930-closed.svg | CURTAIN (id 930) icon; state: closed |
| 940.svg | VERTICAL_BLIND (id 940) icon |
| 940-closed.svg | VERTICAL_BLIND (id 940) icon; state: closed |
| 950.svg | ROLLER_GARAGE_DOOR (id 950) icon |
| 950-closed.svg | ROLLER_GARAGE_DOOR (id 950) icon; state: closed |
| 960.svg | PUMPSWITCH (id 960) icon |
| 960-on.svg | PUMPSWITCH (id 960) icon; state: on |
| 970.svg | HEATORCOLDSOURCESWITCH (id 970) icon |
| 970_1.svg | HEATORCOLDSOURCESWITCH (id 970) icon; alternative icon index 1 |
| 970_1-on.svg | HEATORCOLDSOURCESWITCH (id 970) icon; alternative icon index 1; state: on |
| 970_2.svg | HEATORCOLDSOURCESWITCH (id 970) icon; alternative icon index 2 |
| 970_2-on.svg | HEATORCOLDSOURCESWITCH (id 970) icon; alternative icon index 2; state: on |
| 970_3.svg | HEATORCOLDSOURCESWITCH (id 970) icon; alternative icon index 3 |
| 970_3-on.svg | HEATORCOLDSOURCESWITCH (id 970) icon; alternative icon index 3; state: on |
| 970_4.svg | HEATORCOLDSOURCESWITCH (id 970) icon; alternative icon index 4 |
| 970_4-on.svg | HEATORCOLDSOURCESWITCH (id 970) icon; alternative icon index 4; state: on |
| 970_5.svg | HEATORCOLDSOURCESWITCH (id 970) icon; alternative icon index 5 |
| 970_5-on.svg | HEATORCOLDSOURCESWITCH (id 970) icon; alternative icon index 5; state: on |
| 970-on.svg | HEATORCOLDSOURCESWITCH (id 970) icon; state: on |
| 980.svg | CONTAINER (id 980) icon |
| 980_1.svg | CONTAINER (id 980) icon; alternative icon index 1 |
| 980_1-full.svg | CONTAINER (id 980) icon; alternative icon index 1; fill level: full |
| 980_1-half.svg | CONTAINER (id 980) icon; alternative icon index 1; fill level: half |
| 980_2.svg | CONTAINER (id 980) icon; alternative icon index 2 |
| 980_2-full.svg | CONTAINER (id 980) icon; alternative icon index 2; fill level: full |
| 980_2-half.svg | CONTAINER (id 980) icon; alternative icon index 2; fill level: half |
| 980_3.svg | CONTAINER (id 980) icon; alternative icon index 3 |
| 980_3-full.svg | CONTAINER (id 980) icon; alternative icon index 3; fill level: full |
| 980_3-half.svg | CONTAINER (id 980) icon; alternative icon index 3; fill level: half |
| 980-full.svg | CONTAINER (id 980) icon; fill level: full |
| 980-half.svg | CONTAINER (id 980) icon; fill level: half |
| 981.svg | SEPTIC_TANK (id 981) icon |
| 981_1.svg | SEPTIC_TANK (id 981) icon; alternative icon index 1 |
| 981_1-full.svg | SEPTIC_TANK (id 981) icon; alternative icon index 1; fill level: full |
| 981_1-half.svg | SEPTIC_TANK (id 981) icon; alternative icon index 1; fill level: half |
| 981-full.svg | SEPTIC_TANK (id 981) icon; fill level: full |
| 981-half.svg | SEPTIC_TANK (id 981) icon; fill level: half |
| 982.svg | WATER_TANK (id 982) icon |
| 982_1.svg | WATER_TANK (id 982) icon; alternative icon index 1 |
| 982_1-full.svg | WATER_TANK (id 982) icon; alternative icon index 1; fill level: full |
| 982_1-half.svg | WATER_TANK (id 982) icon; alternative icon index 1; fill level: half |
| 982_2.svg | WATER_TANK (id 982) icon; alternative icon index 2 |
| 982_2-full.svg | WATER_TANK (id 982) icon; alternative icon index 2; fill level: full |
| 982_2-half.svg | WATER_TANK (id 982) icon; alternative icon index 2; fill level: half |
| 982_3.svg | WATER_TANK (id 982) icon; alternative icon index 3 |
| 982_3-full.svg | WATER_TANK (id 982) icon; alternative icon index 3; fill level: full |
| 982_3-half.svg | WATER_TANK (id 982) icon; alternative icon index 3; fill level: half |
| 982-full.svg | WATER_TANK (id 982) icon; fill level: full |
| 982-half.svg | WATER_TANK (id 982) icon; fill level: half |
| 990.svg | CONTAINER_LEVEL_SENSOR (id 990) icon |
| 990-closed.svg | CONTAINER_LEVEL_SENSOR (id 990) icon; state: closed |
| curtains1.svg | Curtains decorative icon variant 1 |
| curtains2.svg | Curtains decorative icon variant 2 |
| curtains3.svg | Curtains decorative icon variant 3 |
| curtains4.svg | Curtains decorative icon variant 4 |
| curtains5.svg | Curtains decorative icon variant 5 |
| sensor.svg | Generic sensor icon |

### Frontend asset icons

| Filename | Description |
| --- | --- |
| moon.svg | moon icon |
| scene.svg | scene icon |
| sun.svg | sun icon |
| sunrise.svg | sunrise icon |
| sunset.svg | sunset icon |
