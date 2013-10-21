#include "BufferedReader.h"

BufferedReader::BufferedReader(Reader &_input, size_t buffSize) :
	buffered(0), readPos(0), bufferSize(buffSize),
			readBuffer(new uint8_t[buffSize]), input(_input) {
}

BufferedReader::~BufferedReader() {
	delete[] readBuffer;
}

int BufferedReader::read() {
	doBuffer();
	if (buffered == 0)
		return -1; // no chars available
	buffered--;
	return readBuffer[readPos++];
}

int BufferedReader::peek() {
	doBuffer();
	if (buffered == 0)
		return -1; // no chars available
	else
		return readBuffer[readPos];
}

int BufferedReader::available() {
	return buffered + input.available();
}

void BufferedReader::doBuffer() {
	// If there are already char in buffer exit
	if (buffered > 0)
		return;

	// Try to buffer as much as possible
	readPos = 0;
	buffered = input.read(readBuffer, bufferSize);
}

size_t BufferedReader::read(uint8_t *buffer, size_t size) {
	// If the internal buffer is empty bypass use
	// direct copy for better performance
	if (buffered == 0)
		return input.read(buffer, size);

	// First read from internal buffer...
	size_t r = 0;
	do {
		buffered--;
		buffer[r++] = readBuffer[readPos++];
		if (r == size)
			return r;
	} while (buffered > 0);

	// Try to full the output with a multi-byte read
	return r + input.read(buffer + r, size - r);
}
