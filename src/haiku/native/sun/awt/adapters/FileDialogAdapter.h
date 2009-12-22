#ifndef FILE_DIALOG_ADAPTER_H
#define FILE_DIALOG_ADAPTER_H

#include "DialogAdapter.h"

class BFilePanel;

class FileDialogAdapter : public DialogAdapter {
private:
	BFilePanel * filePanel;
	BRefFilter * filter;
	bool	waitingForMessage;
	EventEnvironment * environment;

public: 	// initialization
	static BFilePanel * NewFilePanel(JNIEnv * jenv, jobject jpeer, jobject jparent);
	        FileDialogAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BFilePanel * filePanel);

public: 	// destruction
	virtual ~FileDialogAdapter();

private:	// utility functions
	virtual EventEnvironment * Environment();
	        void	SetModal(bool modal);

public: 	// JNI entry points
	virtual void	show();
	virtual void	dispose();
	        void	setFile(char * file);
	        void    setDirectory(char * directory);
	        void	setFilenameFilter(BRefFilter * filter);

protected:	// BFilePanel "entry points"
	virtual void	RefsReceived(BMessage * message);
	virtual void	SaveReceived(BMessage * message);
	virtual void	CancelReceived(BMessage * message);

public: 	// -- insets maintenance
	virtual BRect	GetInsets() { return BRect(0, 0, 0, 0); }
	virtual void	UpdateInsets() { /* block computation */ }

public: 	// -- Field and Method ID cache
	static jfieldID	mode_ID;
	static jfieldID	file_ID;
	static jfieldID	dir_ID;
	static jmethodID hideAndDisposeHandler_ID; // java.awt.Dialog
};

#endif // FILE_DIALOG_ADAPTER_H
