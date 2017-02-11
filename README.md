USBtinLib
=========

USBtinLib is a Java library for accessing USBtin. USBtin is an USB to CAN
interace (http://www.fischl.de/usbtin/).

This is just a custom fork from the main code here: https://github.com/EmbedME/USBtinLib
The author did not merge my pull request with the changes to use maven instead, so keeping my own fork then here.

Build
-----
Maven is used to build the library from Java source code. To create the JAR file,
use
```
mvn clean install
```

USBtinLib depends on jSSC (Java Simple Serial Connector) - library for working
with serial ports from Java. The jSSC library is added automatically by maven when building it
https://code.google.com/p/java-simple-serial-connector/


Usage
-----

Add the following dependency to your maven project:

```
  <dependency>
    <groupId>de.fischl.usbtin</groupId>
    <artifactId>usbtin</artifactId>
    <version>1.0-SNAPSHOT</version>
  </dependency>
```

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


Filters
-------

USBtin supports hardware filtering. Especially on high loaded busses this function is useful to
reduce traffic between USBtin and the host computer. Up to two filter chains (combination of
filter mask and filter set) are supported. The filter chain length is limited to 2 for the first
chain and 4 for the second one. Please consult the MCP2515 datasheet for details on filter mechanism
(section "Message Acceptance Filters and Masks").

Using filters is optional. On startup all messages are accepted.

Example - Accept standard CAN messages with identifier 0x123 and data byte0: 0x45 or 0x67:
```
usbtin.connect(...);
usbtin.setFilter(new FilterChain[] {
    new FilterChain(
        new FilterMask(0x7ff, (byte)0xff, (byte)0x00),
        new FilterValue[] {
            new FilterValue(0x123, (byte)0x45, (byte)0x00),
            new FilterValue(0x123, (byte)0x67, (byte)0x00)
        }
    )
});
usbtin.openCANChannel(...);
```

Example - Accept standard CAN messages with identifiers 0x050, 0x080, 0x120 - 0x12f:
```
usbtin.connect(...);
usbtin.setFilter(new FilterChain[] {
    new FilterChain(
        new FilterMask(0x7f0, (byte)0x00, (byte)0x00),
        new FilterValue[] {
            new FilterValue(0x120, (byte)0x00, (byte)0x00)
        }
    ),
    new FilterChain(
        new FilterMask(0x7ff, (byte)0x00, (byte)0x00),
        new FilterValue[] {
            new FilterValue(0x050, (byte)0x00, (byte)0x00),
            new FilterValue(0x080, (byte)0x00, (byte)0x00)
        }
    )
});
usbtin.openCANChannel(...);
```

Example - Accept extended CAN messages with identifiers 0x12345670 - 0x1234567f:
```
usbtin.connect(...);
usbtin.setFilter(new FilterChain[] {
    new FilterChain(
        new FilterMask(0x1ffffff0),
        new FilterValue[] {
            new FilterValue(0x12345670)
        }
    )
});
usbtin.openCANChannel(...);
```


Changelog
---------

1.1.0 (2015-10-03)
* Added: Allow baudrates without preset (baudrate setting auto calculation).
* Added: CAN message filtering. setFilter(...).
* Added: Clear error flags on connect(...).
* Fixed: First incoming message was ignored. Filter some special characters in serialEvent(...).

1.0.0 (2014-12-04)
First release


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
