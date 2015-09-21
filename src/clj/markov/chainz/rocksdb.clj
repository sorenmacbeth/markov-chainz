(ns markov.chainz.rocksdb
  (:refer-clojure :exclude [get])
  (:import [org.rocksdb RocksDB Options CompressionType CompactionStyle Statistics
            Filter ReadOptions WriteOptions WriteBatch RocksIterator]
           [org.rocksdb.util SizeUnit]))

;; ## Options
(defn ^Options options []
  (Options.))

(defn ^Options create-if-missing! [^Options options val]
  (.setCreateIfMissing options val))

(defn ^Options create-statistics! [^Options options]
  (.createStatistics options))

(defn ^Options write-buffer-size! [^Options options size]
  (.setWriteBufferSize options size))

(defn ^Options max-write-buffer-number! [^Options options val]
  (.setMaxWriteBufferNumber options val))

(defn ^Options max-background-compactions! [^Options options val]
  (.setMaxBackgroundCompactions options val))

(defn ^Options compression-type! [^Options options val]
  (.setCompressionType options val))

(defn ^Options compaction-style! [^Options options val]
  (.setCompactionStyle options val))

;; ## Read Options
(defn read-options []
  (ReadOptions.))

;; ## Write Options
(defn write-options []
  (WriteOptions.))

;; ## Write Batch
(defn write-batch []
  (WriteBatch.))

;; ## RocksDB
(defn open
  ([path]
   (open (-> (options) (create-if-missing! true) (create-statistics!)) path))
  ([^Options options path]
   (RocksDB/open options path)))

(defn get [^RocksDB db ^bytes k]
  (.get db k))

(defn multi-get [^RocksDB db ks]
  (.multiGet db ks))

(defn put [^RocksDB db ^bytes k ^bytes v]
  (.put db k v))

(defn close [^RocksDB db]
  (.close db))
