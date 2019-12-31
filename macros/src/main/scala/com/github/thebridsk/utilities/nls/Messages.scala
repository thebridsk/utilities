package com.github.thebridsk.utilities.nls

import java.util.ResourceBundle
import java.util.MissingResourceException
import com.github.thebridsk.utilities.message.Message
import java.util.Locale

object Messages {

  val BUNDLE_NAME = "com.github.thebridsk.utilities.nls.messages"; //$NON-NLS-1$

  val RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

  /**
    * @param key
    * @return The NLS message
    */
  def getString(key: String) = {
    try {
      RESOURCE_BUNDLE.getString(key);
    } catch {
      case _: MissingResourceException =>
        s"!{key}!"
    }
  }

  /**
    * @param key
    * @param args
    * @return the NLS message
    */
  def getString(key: String, args: Any*) = {
    Message.getNLSMessage(
      RESOURCE_BUNDLE,
      BUNDLE_NAME,
      Locale.getDefault(),
      key,
      args: _*
    );
  }

  /**
    * Create a message using the bundle name {@link Messages#BUNDLE_NAME}
    * @param key
    * @param args
    * @return the {@link Message} object.
    */
  def get(key: String, args: Any*) = {
    new Message(Messages.BUNDLE_NAME, key, args: _*);
  }

}
