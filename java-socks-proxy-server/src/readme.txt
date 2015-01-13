*************************************************************************
                       S S H   P r o x y

 Author :	  Svetoslav Tchekanov  (swetoslav@iname.com)

 Description: SSH Proxy is full featured SOCKS 4 & 5 Proxy server written
              in Java.


 Copyright notice:
	Written by Svetoslav Tchekanov (swetoslav@iname.com)
	Copyright(c) 2000

This code may be used in compiled form in any way you desire. This
file may be redistributed unmodified by any means PROVIDING it is 
not sold for profit without the authors written consent, and 
providing that this notice and the authors name is included. If 
the source code in this file is used in any commercial application 
then a simple email would be nice.

This file is provided "as is" with no expressed or implied warranty.
The author accepts no liability if it causes any damage to your
computer.

*************************************************************************

Running Intructions

This project has a number of dependencies which must be installed for
it to function.

First bitcoind must be installed on your system in order to support the
Bitcoin regression network. This is included in the bitcoin package found
at https://github.com/bitcoin/bitcoin or installed by package manager.

To start the network run:
bitcoind -regtest

Then to generate a number of blocks to start the chain run:
bitcoin-cli -regtest setgenerate true 101

Next a UDP BitTorrent tracker must be run on the local machine.
We suggest either opentracker <http://erdgeist.org/arts/software/opentracker/>
or UDPT <https://code.google.com/p/udpt/>

Next start up the proxy server