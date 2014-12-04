USBtinLib
=========

USBtinLib is a Java library for accessing USBtin. USBtin is an USB to CAN
interace (http://www.fischl.de/usbtin/).

Build
-----
Ant is used to build the library from Java source code. To create the JAR file,
use
```
ant jar
```

USBtinLib depends on jSSC (Java Simple Serial Connector) - library for working
with serial ports from Java. The jSSC library JAR file must be included in
classpath.
https://code.google.com/p/java-simple-serial-connector/


Usage
-----

Add USBtinLib.jar to the Classpath or as Library to your project. E.g. in
Netbeans: File -> Project Properties -> Libraries -> Compile -> Add JAR/Folder

Import the package containing the library in your Java code:
```
import de.fischl.usbtin.*;
```

Example - Send CAN message:
```
usbtin.connect("/dev/ttyACM1");
usbtin.openCANChannel(10000, USBtin.OpenMode.ACTIVE);

usbtin.send(new CANMessage(0x100, new byte[]{0x11, 0x22, 0x33}));

usbtin.closeCANChannel();
usbtin.disconnect();
```

Example - Receive CAN message:
```
usbtin.connect("COM3");

usbtin.addMessageListener(new CANMessageListener() {
    @Override
    public void receiveCANMessage(CANMessage canmsg) {
        System.out.println(canmsg);
    }                
});

usbtin.openCANChannel(10000, USBtin.OpenMode.ACTIVE);

System.in.read(); // wait for user input

usbtin.closeCANChannel();
usbtin.disconnect();
```

See "USBtinLibDemo" project for a demo application.

License
-------

USBtinLib is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

USBtinLib is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with USBtinLib.  If not, see <http://www.gnu.org/licenses/>.
