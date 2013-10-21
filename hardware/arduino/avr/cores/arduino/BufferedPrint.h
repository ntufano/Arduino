
#ifndef BufferedPrint_h
#define BufferedPrint_h

#include "Print.h"

class BufferedPrint : public Print {
public:
	static const int DEFAULT_BUFFER_SIZE = 32;
	BufferedPrint(Print &_output, size_t buffSize=DEFAULT_BUFFER_SIZE);
	~BufferedPrint();

	// Print methods
    size_t write(uint8_t);
    size_t write(const uint8_t *buffer, size_t size);
    void flush();

protected:
	uint8_t buffered;
	uint8_t bufferSize;
	uint8_t *writeBuffer;
	Print &output;
};

#endif
