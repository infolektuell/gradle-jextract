#include "hello.h"

extern "C" {
    const char *hello_world(void)
    {
        return "Hello World";
    }
}
