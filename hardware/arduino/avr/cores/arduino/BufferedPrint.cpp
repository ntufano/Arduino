#include "BufferedPrint.h"

BufferedPrint::BufferedPrint(Print &_output, size_t buffSize) :
	buffered(0), bufferSize(buffSize), writeBuffer(new uint8_t[buffSize]),
			output(_output) {
}

BufferedPrint::~BufferedPrint() {
	delete[] writeBuffer;
}

size_t BufferedPrint::write(uint8_t c) {
	writeBuffer[buffered++] = c;
	if (buffered == bufferSize)
		flush();
}

size_t BufferedPrint::write(const uint8_t *buff, size_t size) {
	size_t sent = 0;
	while (sent < size) {
		writeBuffer[buffered++] = *buff++;
		sent++;
		if (buffered == bufferSize)
			flush();
	}
	return sent;
}

void BufferedPrint::flush() {
	output.write(writeBuffer, buffered);
	buffered = 0;
}
