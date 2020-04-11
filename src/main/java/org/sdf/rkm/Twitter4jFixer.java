package org.sdf.rkm;

import twitter4j.StatusListener;
import twitter4j.TwitterStream;

class Twitter4jFixer {
  public static void addListener(TwitterStream stream, StatusListener listener) {
    stream.addListener(listener);
  }
}

