DeviceHive Android Things Demo
=========================

[DeviceHive]: http://devicehive.com "DeviceHive framework"
[DataArt]: http://dataart.com "DataArt"

DeviceHive Android Things Demo demostrates the usage of [DeviceHive Java Library](https://github.com/devicehive/devicehive-java) with [Android Things](https://developer.android.com/things/index.html) Project. 
 This project was created to control the WS2812B LED strip mode and communicate with IoT service. It can generate random color pattern or generate a color pattern that depends on audio record that was captured by the phone's mic.
This project includes two modules: `mobile` and `things`.
To make this module work you just should specify next properties in `CommandService.java`: 

```java
    public static final String URL = "";
    public static final String REFRESH_TOKEN = "";
    public static final String DEVICE_ID = "";
```
Then this module can be installed on you board that supports Android Things.


Things module
------------------
The `things` module was built on `Raspberry Pi 3` to control the WS2812B LED strip via [SPI Protocol](https://github.com/Nikolay-Kha/android-things-ws2812b). 
The app is listening for commands from WebSocket connection established with DeviceHive server.

Mobile module
------------------
The `mobile` module sends commands to Raspberry Pi via DeviceHive server.
You don't need to specify any properties inside this module. You just need to install it on your mobile device. On the login screen you'd need to enter your credentials and the server url.
The mobile application can send several types of commands: 
* BLINK
* RANDOM
* AUDIO
* OFF

`BLINK` command just  turns on leds one by one. The leds are "running" outside from the center of LED Strip and back. 
`RANDOM` command generates random HSV colors to emulate a ChristmasLight effect.
`AUDIO` command generates colors based on Audio signal that was recorded on Mobile Device and transformed with [Fast Fourier Transformation](https://en.wikipedia.org/wiki/Fast_Fourier_transform) algorithm (FFT).
`OFF` command just turns off the LED Strip.





DeviceHive license
------------------

[DeviceHive] is developed by [DataArt] Apps and distributed under Open Source
[Apache 2.0](https://en.wikipedia.org/wiki/Apache_License). This basically means
you can do whatever you want with the software as long as the copyright notice
is included. This also means you don't have to contribute the end product or
modified sources back to Open Source, but if you feel like sharing, you are
highly encouraged to do so!

&copy; Copyright 2017 DataArt Apps &copy; All Rights Reserved
