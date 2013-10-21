
#ifndef BufferedReader_h
#define BufferedReader_h

#include "Reader.h"

class BufferedReader : public Reader {
public:
	static const int DEFAULT_BUFFER_SIZE = 32;
	BufferedReader(Reader &_input, size_t buffSize=DEFAULT_BUFFER_SIZE);
	~BufferedReader();

	// Reader methods
    int available();
    int read();
    size_t read(uint8_t *buffer, size_t size);
    int peek();

protected:
	void doBuffer();
	uint8_t buffered;
	uint8_t readPos;
	uint8_t bufferSize;
	uint8_t *readBuffer;
	Reader &input;
};

#endif
