package com.pulse.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile LectureDao _lectureDao;

  private volatile NoteDao _noteDao;

  private volatile NoteVisualDao _noteVisualDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(11) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `lectures` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `videoId` TEXT, `pdfId` TEXT, `pdfLocalPath` TEXT NOT NULL, `videoLocalPath` TEXT, `isPdfDownloaded` INTEGER NOT NULL, `isLocal` INTEGER NOT NULL, `lastPosition` INTEGER NOT NULL, `videoDuration` INTEGER NOT NULL, `speed` REAL NOT NULL, `isFavorite` INTEGER NOT NULL, `pdfPageCount` INTEGER NOT NULL, `hlcTimestamp` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `lectureId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `text` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `hlcTimestamp` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, FOREIGN KEY(`lectureId`) REFERENCES `lectures`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_lectureId` ON `notes` (`lectureId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `note_visuals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `lectureId` TEXT NOT NULL, `pdfId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `pageNumber` INTEGER NOT NULL, `type` TEXT NOT NULL, `data` TEXT NOT NULL, `color` INTEGER NOT NULL, `strokeWidth` REAL NOT NULL, `alpha` REAL NOT NULL, `hlcTimestamp` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`lectureId`) REFERENCES `lectures`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_visuals_lectureId` ON `note_visuals` (`lectureId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_visuals_pdfId` ON `note_visuals` (`pdfId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '94871f13976c118be132c9b07463cf31')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `lectures`");
        db.execSQL("DROP TABLE IF EXISTS `notes`");
        db.execSQL("DROP TABLE IF EXISTS `note_visuals`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsLectures = new HashMap<String, TableInfo.Column>(16);
        _columnsLectures.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLectures.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLectures.put("videoId", new TableInfo.Column("videoId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLectures.put("pdfId", new TableInfo.Column("pdfId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLectures.put("pdfLocalPath", new TableInfo.Column("pdfLocalPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLectures.put("videoLocalPath", new TableInfo.Column("videoLocalPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLectures.put("isPdfDownloaded", new TableInfo.Column("isPdfDownloaded", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLectures.put("isLocal", new TableInfo.Column("isLocal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLectures.put("lastPosition", new TableInfo.Column("lastPosition", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLectures.put("videoDuration", new TableInfo.Column("videoDuration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLectures.put("speed", new TableInfo.Column("speed", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLectures.put("isFavorite", new TableInfo.Column("isFavorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLectures.put("pdfPageCount", new TableInfo.Column("pdfPageCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLectures.put("hlcTimestamp", new TableInfo.Column("hlcTimestamp", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLectures.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLectures.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLectures = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLectures = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLectures = new TableInfo("lectures", _columnsLectures, _foreignKeysLectures, _indicesLectures);
        final TableInfo _existingLectures = TableInfo.read(db, "lectures");
        if (!_infoLectures.equals(_existingLectures)) {
          return new RoomOpenHelper.ValidationResult(false, "lectures(com.pulse.core.data.db.Lecture).\n"
                  + " Expected:\n" + _infoLectures + "\n"
                  + " Found:\n" + _existingLectures);
        }
        final HashMap<String, TableInfo.Column> _columnsNotes = new HashMap<String, TableInfo.Column>(7);
        _columnsNotes.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("lectureId", new TableInfo.Column("lectureId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("text", new TableInfo.Column("text", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("hlcTimestamp", new TableInfo.Column("hlcTimestamp", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysNotes = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysNotes.add(new TableInfo.ForeignKey("lectures", "CASCADE", "NO ACTION", Arrays.asList("lectureId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesNotes = new HashSet<TableInfo.Index>(1);
        _indicesNotes.add(new TableInfo.Index("index_notes_lectureId", false, Arrays.asList("lectureId"), Arrays.asList("ASC")));
        final TableInfo _infoNotes = new TableInfo("notes", _columnsNotes, _foreignKeysNotes, _indicesNotes);
        final TableInfo _existingNotes = TableInfo.read(db, "notes");
        if (!_infoNotes.equals(_existingNotes)) {
          return new RoomOpenHelper.ValidationResult(false, "notes(com.pulse.core.data.db.Note).\n"
                  + " Expected:\n" + _infoNotes + "\n"
                  + " Found:\n" + _existingNotes);
        }
        final HashMap<String, TableInfo.Column> _columnsNoteVisuals = new HashMap<String, TableInfo.Column>(13);
        _columnsNoteVisuals.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNoteVisuals.put("lectureId", new TableInfo.Column("lectureId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNoteVisuals.put("pdfId", new TableInfo.Column("pdfId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNoteVisuals.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNoteVisuals.put("pageNumber", new TableInfo.Column("pageNumber", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNoteVisuals.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNoteVisuals.put("data", new TableInfo.Column("data", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNoteVisuals.put("color", new TableInfo.Column("color", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNoteVisuals.put("strokeWidth", new TableInfo.Column("strokeWidth", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNoteVisuals.put("alpha", new TableInfo.Column("alpha", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNoteVisuals.put("hlcTimestamp", new TableInfo.Column("hlcTimestamp", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNoteVisuals.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNoteVisuals.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysNoteVisuals = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysNoteVisuals.add(new TableInfo.ForeignKey("lectures", "CASCADE", "NO ACTION", Arrays.asList("lectureId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesNoteVisuals = new HashSet<TableInfo.Index>(2);
        _indicesNoteVisuals.add(new TableInfo.Index("index_note_visuals_lectureId", false, Arrays.asList("lectureId"), Arrays.asList("ASC")));
        _indicesNoteVisuals.add(new TableInfo.Index("index_note_visuals_pdfId", false, Arrays.asList("pdfId"), Arrays.asList("ASC")));
        final TableInfo _infoNoteVisuals = new TableInfo("note_visuals", _columnsNoteVisuals, _foreignKeysNoteVisuals, _indicesNoteVisuals);
        final TableInfo _existingNoteVisuals = TableInfo.read(db, "note_visuals");
        if (!_infoNoteVisuals.equals(_existingNoteVisuals)) {
          return new RoomOpenHelper.ValidationResult(false, "note_visuals(com.pulse.core.data.db.NoteVisual).\n"
                  + " Expected:\n" + _infoNoteVisuals + "\n"
                  + " Found:\n" + _existingNoteVisuals);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "94871f13976c118be132c9b07463cf31", "b2ebc76bfa516e2d28803da43a69b6c2");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "lectures","notes","note_visuals");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `lectures`");
      _db.execSQL("DELETE FROM `notes`");
      _db.execSQL("DELETE FROM `note_visuals`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(LectureDao.class, LectureDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(NoteDao.class, NoteDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(NoteVisualDao.class, NoteVisualDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public LectureDao lectureDao() {
    if (_lectureDao != null) {
      return _lectureDao;
    } else {
      synchronized(this) {
        if(_lectureDao == null) {
          _lectureDao = new LectureDao_Impl(this);
        }
        return _lectureDao;
      }
    }
  }

  @Override
  public NoteDao noteDao() {
    if (_noteDao != null) {
      return _noteDao;
    } else {
      synchronized(this) {
        if(_noteDao == null) {
          _noteDao = new NoteDao_Impl(this);
        }
        return _noteDao;
      }
    }
  }

  @Override
  public NoteVisualDao noteVisualDao() {
    if (_noteVisualDao != null) {
      return _noteVisualDao;
    } else {
      synchronized(this) {
        if(_noteVisualDao == null) {
          _noteVisualDao = new NoteVisualDao_Impl(this);
        }
        return _noteVisualDao;
      }
    }
  }
}
