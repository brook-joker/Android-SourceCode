package com.sun.corba.se.spi.activation;

/**
* com/sun/corba/se/spi/activation/ServerAlreadyUninstalledHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /Users/java_re/workspace/8-2-build-macosx-x86_64/jdk8u92/6642/corba/src/share/classes/com/sun/corba/se/spi/activation/activation.idl
* Thursday, March 31, 2016 9:24:49 PM PDT
*/

public final class ServerAlreadyUninstalledHolder implements org.omg.CORBA.portable.Streamable
{
  public com.sun.corba.se.spi.activation.ServerAlreadyUninstalled value = null;

  public ServerAlreadyUninstalledHolder ()
  {
  }

  public ServerAlreadyUninstalledHolder (com.sun.corba.se.spi.activation.ServerAlreadyUninstalled initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = com.sun.corba.se.spi.activation.ServerAlreadyUninstalledHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    com.sun.corba.se.spi.activation.ServerAlreadyUninstalledHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return com.sun.corba.se.spi.activation.ServerAlreadyUninstalledHelper.type ();
  }

}
