#ifndef LABEL_ADAPTER_H
#define LABEL_ADAPTER_H

#include "ComponentAdapter.h"
#include <InterfaceDefs.h> // for alignment
#include <interface/Point.h>

class Label;

class LabelAdapter : public ComponentAdapter {
private:
	static Label * NewLabel(JNIEnv * jenv, jobject jpeer, jobject jparent);
	Label * label;

public: 	// initialization
	        LabelAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// destruction
	virtual ~LabelAdapter();
	
public: 	// JNI entry points
	virtual void	enable();
	virtual void	disable();
	        void	setText(char * text);
	        void	setAlignment(alignment flag);

	        BPoint	getMinimumSize();

public: 	// -- Field and Method ID cache
	static jfieldID text_ID;
	static jfieldID alignment_ID;
};

#endif // LABEL_ADAPTER_H
