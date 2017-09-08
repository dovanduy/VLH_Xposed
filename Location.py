# -*- coding: utf8 -*-

import socket as SOCKET
import os as OS

sHost = '192.168.0.5'
nPort = 15555
nBuff_Size = 4096

def getLocation() :
	FILE = open( 'Config/Location.conf', 'r' )
	sContent = FILE.readline().split()

	sLatitude = sContent[0] + '\n'
	sLongitude = sContent[1] + '\n'

	return sLatitude, sLongitude

def main() :
	oSocketServer = SOCKET.socket( SOCKET.AF_INET, SOCKET.SOCK_STREAM )
	oSocketServer.setsockopt( SOCKET.SOL_SOCKET, SOCKET.SO_REUSEADDR, 1 )
	oSocketServer.bind( ( sHost, nPort ) )
	oSocketServer.listen( 5 )

	print 'Socket is created, Listening ...\n'

	while True :
		oClient, oAddress = oSocketServer.accept()
		# print 'Client connected.'

		# RECEIVE initial message
		bData = oClient.recv( nBuff_Size )

		# CHECK service is on/off
		with open( 'Config/ServiceStatus.conf', 'r' ) as FILE_SERVICE_STATUS :
			sContent = FILE_SERVICE_STATUS.read()
			nIndex = sContent.find( 'LOCATION' )
			bStatus = int( sContent[nIndex+9:nIndex+10] )

		if bStatus == True :
			tLocation = getLocation()

			# REPLY back to user
			oClient.send( tLocation[0] )
			oClient.send( tLocation[1] )
			# print 'Get Location: ' + tLocation[0].strip( '\n' ) + ', ' + tLocation[1].strip( '\n' )
		else :
			# print 'Service is off, forward to Controller'

			# ASK controller for service location (IP, PORT)
			oSocketClient = SOCKET.socket( SOCKET.AF_INET, SOCKET.SOCK_STREAM )
			oSocketClient.connect( ( '140.112.28.135', 15544 ) )
			oSocketClient.send( 'LOCATION' )
			bResponse = oSocketClient.recv( nBuff_Size ).split()
			oSocketClient.close()

			# REDIRECT to destnation and GET response
			oSocketClient = SOCKET.socket( SOCKET.AF_INET, SOCKET.SOCK_STREAM )
			oSocketClient.connect( ( str( bResponse[0] ), int( bResponse[1] ) ) )
			oSocketClient.send( 'UPDATE' )
			bData1 = oSocketClient.recv( 1024 )
			bData2 = oSocketClient.recv( 1024 )
			oSocketClient.close()

			# REPLY back to user
			oClient.send( bData1 )
			oClient.send( bData2 )

		# print '---------------------------------\n'

		oClient.close()

if __name__ == '__main__' :
	main()
