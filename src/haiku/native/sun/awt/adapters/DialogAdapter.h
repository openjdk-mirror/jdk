#ifndef DIALOG_ADAPTER_H
#define DIALOG_ADAPTER_H

#include "WindowAdapter.h"

class DialogAdapter : public WindowAdapter {
public: 	// initialization
	static BWindow * NewDialog(JNIEnv * jenv, jobject jpeer, jobject jparent);
	        DialogAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BWindow * window);

public: 	// destruction
	virtual ~DialogAdapter();

private:	// utility functions
	        void	SetModal(bool modal);
	
public: 	// JNI entry points
	virtual void	show();
	virtual void	hide();
	        void    createFullSubset();
	        void	removeFromSubset(BWindow * window);
};

#endif // DIALOG_ADAPTER_H
