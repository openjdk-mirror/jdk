#ifndef RECTANGLE_H
#define RECTANGLE_H

#include <jni.h>
#include <jni_util.h>

class Rectangle {
public:
	/* java.awt.Rectangle field ids */
	static jfieldID x_ID;
	static jfieldID y_ID;
	static jfieldID width_ID;
	static jfieldID height_ID;
};

#endif // RECTANGLE_H
