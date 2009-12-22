#ifndef APPLICATION_H
#define APPLICATION_H

// Haiku Includes...
#include <app/Application.h>
#include <interface/Font.h>
#include <Handler.h>
#include <InterfaceDefs.h>
#include <interface/Menu.h>
#include <Message.h>
#include <OS.h>
#include <Roster.h>
#include <Screen.h>
#include <interface/Window.h>

#define JAVA_APP_SIG "application/x-vnd.haiku-java"

class Application : public BApplication {
	public:
		Application();
		~Application();
		
		virtual void Quit();
};
#endif // APPLICATION_H
