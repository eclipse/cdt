
#include "DateTime.hh"
#include <QGuiApplication>
#include <QtQuick>

int main( int argc, char * argv[] )
{
    QGuiApplication app(argc, argv);

    DateTime datetime;

    QQuickView view;
    view.rootContext()->setContextProperty( "datetimeModel", &datetime );
    view.setSource( QStringLiteral( "$(baseName).qml" ) );
    view.show();

    app.connect( view.engine(), SIGNAL( quit() ), SLOT( quit() ) );

    return app.exec();
}
