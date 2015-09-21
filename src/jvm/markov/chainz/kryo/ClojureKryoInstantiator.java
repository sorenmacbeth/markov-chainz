package markov.chainz.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.twitter.chill.KryoInstantiator;
import com.twitter.chill.KryoPool;
import carbonite.JavaBridge;
import com.esotericsoftware.kryo.Kryo;

public class ClojureKryoInstantiator extends KryoInstantiator {
  static final Object _mutex =  new Object();
  transient KryoPool _kpool = null;

  @Override
  public Kryo newKryo() {
    try {
      return JavaBridge.defaultRegistry();
    } catch (Exception e) {
      throw new RuntimeException("unable to create new Kryo: " + e);
    }
  }

  public KryoPool defaultPool() {
    synchronized(_mutex) {
      if (_kpool == null) {
        _kpool = KryoPool.withByteArrayOutputStream(guessThreads(), new ClojureKryoInstantiator());
      }
      return _kpool;
    }
  }

  private int guessThreads() {
    int GUESS_THREADS_PER_CORE = 4;
    int cores = Runtime.getRuntime().availableProcessors();
    return cores * GUESS_THREADS_PER_CORE;
  }
}
