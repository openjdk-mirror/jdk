#ifndef TEXT_AREA_ADAPTER_H
#define TEXT_AREA_ADAPTER_H

#include "TextComponentAdapter.h"
#include <interface/Point.h>

class TextArea;

class TextAreaAdapter : public TextComponentAdapter {
private:
	TextArea * textArea;

private:	// initialization
	static TextArea * NewTextArea(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// initialization
	        TextAreaAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// destruction
	virtual ~TextAreaAdapter();

public: 	// JNI entry points
	virtual BPoint	getPreferredSize();
	virtual BPoint	getMinimumSize();
	        void	insert(char * text, int pos);
	        void	replaceRange(char * text, int start, int end);
	        BPoint	getPreferredSize(int rows, int columns);
	        BPoint	getMinimumSize(int rows, int columns);

public: 	// -- Field and Method ID cache
	static jfieldID  scrollbarVisibility_ID;
};

#endif // TEXT_AREA_ADAPTER_H
