package com.example.legolist

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import java.io.*

import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class DatabaseHelper(private val myContext: Context):
    SQLiteOpenHelper(myContext, DB_NAME, null, 10) {
    var DB_PATH: String? = null
    private var myDataBase: SQLiteDatabase? = null

    @Throws(IOException::class)
    fun createDataBase() {
        val dbExist = checkDataBase()
        if (dbExist) {
        } else {
            this.readableDatabase
            try {
                copyDataBase()
            } catch (e: IOException) {
                throw Error("Error copying database")
            }
        }
    }

    fun addInventoryPart(inventoryID:Int, typeID:String, itemID:String, quantityInSet:Int, quantityInStore:Int, colorID:String, extra:String) {
        val values = ContentValues()
        values.put("InventoryID", inventoryID)
        values.put("TypeID", typeID)
        values.put("ItemID", itemID)
        values.put("QuantityInSet", quantityInSet)
        values.put("QuantityInStore", quantityInStore)
        values.put("ColorID", colorID)
        values.put("Extra", extra)
        val db = this.writableDatabase
        db.insert("InventoriesParts", null, values)
        db.close()
    }
    fun addProject(ProjectNumber:Int, ProjectName:String) {
        val values = ContentValues()
        values.put("id", ProjectNumber)
        values.put("Name", ProjectName)
        values.put("LastAccessed", Calendar.getInstance().time.time)
        values.put("Active", 1)
        val db = this.writableDatabase
        db.insert("Inventories", null, values)
        db.close()
    }

    fun findProjects () :  MutableList<Project>{
        val db = this.writableDatabase
        val query = "SELECT * FROM Inventories ORDER BY LastAccessed DESC"
        val cursor = db.rawQuery(query, null)
        val projects: MutableList<Project> = mutableListOf()
        projects.clear()

        if (cursor.moveToFirst()) {
            do {
                val part = Project(cursor.getInt(0), cursor.getString(1), cursor.getInt(2), cursor.getInt(3))
                projects.add(part)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return projects
    }

    fun archiveProject(id: Int) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put("Active", 0)
        db.update("Inventories", values, "id = $id", null)
        db.close()
    }

    fun existProject (id: Int): Boolean {
        var result = false
        val query = "SELECT * FROM Inventories WHERE id = $id"

        val db = this.writableDatabase
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            result = true
        }
        cursor.close()
        db.close()
        return result
    }

    fun deleteProject (id:Int) {
        val db = this.writableDatabase
        db.delete("Inventories", "id = ?", arrayOf(id.toString()))
        db.delete("InventoriesParts", "InventoryID = ?", arrayOf(id.toString()))
        db.close()
    }

    fun findProjectParts (id: Int): MutableList<ProjectPart> {
        val db = this.writableDatabase
        val query = "SELECT * FROM InventoriesParts WHERE InventoryID = $id"
        val cursor = db.rawQuery(query, null)
        val projectsParts: MutableList<ProjectPart> = mutableListOf()
        projectsParts.clear()

        if (cursor.moveToFirst()) {
            do {
                val projectPart = ProjectPart(
                    cursor.getInt(0), cursor.getInt(1),
                    cursor.getString(2), cursor.getString(3),
                    cursor.getInt(4), cursor.getInt(5),
                    cursor.getString(6), cursor.getString(7)
                )
                projectsParts.add(projectPart)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return projectsParts
    }
    
    fun changeQuantity ( id:Int, number:Int): Int {
        var inStore = 0
        var inSet = 0
        val db = this.writableDatabase
        val query = "SELECT * FROM InventoriesParts WHERE id = $id"
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst())
        {
            inSet = cursor.getInt(4)
            inStore = cursor.getInt(5)
            if (inStore + number in 0..inSet) {
                val values = ContentValues()
                values.put("QuantityInStore", inStore + number)
                db.update("InventoriesParts", values, "id = $id", null)
                cursor.close()
                db.close()
                return inStore + number
            }
        }
        
        cursor.close()
        db.close()
        return -1
    }
    
    fun checkIfActive (id: Int): Int {
        val db = this.writableDatabase
        val query = "SELECT * FROM Inventories WHERE id = \"$id\""
        val cursor = db.rawQuery(query, null)
        cursor.moveToFirst()
        return cursor.getInt(2)
    }
    
    fun findInfo(code: String, color: String): String {
        val db = this.writableDatabase

        val query1 = "SELECT * FROM Parts WHERE Code = \"$code\""
        val query2 = "SELECT * FROM Colors WHERE Code = \"$color\""

        val cursor1 = db.rawQuery(query1, null)
        val cursor2 = db.rawQuery(query2, null)

        var end = ""

        if (cursor1.moveToFirst()) end += cursor1.getString(3)
        if (cursor2.moveToFirst() && color != "0") end += "\n" + cursor2.getString(2)
        end += " [$code]"

        cursor1.close()
        cursor2.close()
        db.close()

        return end
    }
    fun getPicture(itemID: String, color: String) {
        var image: ByteArray? = null
        var url = ""
        val db = this.writableDatabase

        val query = "SELECT * FROM Parts WHERE Code = \"$itemID\""
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            val id = Integer.parseInt(cursor.getString(0))
            cursor.close()

            val query2 = "SELECT * FROM Codes WHERE ItemID = $id AND ColorID = \"$color\""
            val cursor2 = db.rawQuery(query2, null)

            if (cursor2.moveToFirst()) {


                val codesCode = Integer.parseInt(cursor2.getString(3))
                val codesID = Integer.parseInt(cursor2.getString(0))

                image = cursor2.getBlob(4)
                cursor2.close()

                if (image == null) {
                    try {
                        url = "https://www.lego.com/service/bricks/5/2/$codesCode"
                        image = downloadPicture(url)
                    } catch (e: Exception) {

                    }
                    if (image == null) {
                        try {
                            url = "http://img.bricklink.com/P/$color/$itemID.gif"
                            image = downloadPicture(url)
                        } catch (e: Exception) {
                        }
                    }
                    if (image != null) {
                        val values = ContentValues()
                        values.put("Image", image)
                        db.update("Codes", values, "id = $codesID", null)
                    }
                }
            } else {
                val query3 = "SELECT * FROM Codes ORDER BY \"id\" DESC"
                val cursor3 = db.rawQuery(query3, null)

                cursor3.moveToFirst()
                val n = Integer.parseInt(cursor3.getString(0)) + 1

                try {
                    url = "http://img.bricklink.com/P/$color/$itemID.gif"
                    image = downloadPicture(url)

                } catch (e: Exception) {
                    try {
                        url = "https://www.bricklink.com/PL/$itemID.jpg"
                        image = downloadPicture(url)
                    } catch (e: Exception) { }
                }

                val values = ContentValues()
                values.put("id", n)
                values.put("ItemID", itemID)
                values.put("ColorID", color)
                values.put("Image", image)
                db.insert("Codes", null, values)
                cursor3.close()
            }
        }
        db.close()
    }

    private fun downloadPicture(url: String): ByteArray? {
        return try {
            val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input: InputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.toByteArray()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    fun findBrickPicture (itemID: String, color: String): ByteArray? {
        var image: ByteArray? = null
        val db = this.writableDatabase
        val query = "SELECT * FROM Parts WHERE Code = \"$itemID\""
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            val id = Integer.parseInt(cursor.getString(0))
            val query2 = "SELECT * FROM Codes WHERE ItemID = $id AND ColorID = \"$color\""
            val cursor2 = db.rawQuery(query2, null)
            if (cursor2.moveToFirst()) {
                image = cursor2.getBlob(4)
            }
            cursor2.close()
        }

        if (image == null) {
            val query3 = "SELECT * FROM Codes WHERE ItemID = \"$itemID\" AND ColorID = \"$color\""
            val cursor3 = db.rawQuery(query3, null)
            if (cursor3.moveToFirst()) {
                image = cursor3.getBlob(4)
            }
            cursor3.close()
        }

        cursor.close()
        db.close()
        return image

    }

    fun updateLastAccessed(id: Int) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put("LastAccessed", Calendar.getInstance().time.time)
        db.update("Inventories", values, "id = $id", null)
        db.close()
    }

    fun getQuantity(id: Int): Int {
        val db = this.writableDatabase
        val query = "SELECT * FROM InventoriesParts WHERE id = $id"
        val cursor = db.rawQuery(query, null)
        cursor.moveToFirst()
        val quantity = cursor.getInt(4) - cursor.getInt(5)
        cursor.close()
        db.close()
        return quantity
    }

    private fun checkDataBase(): Boolean {
        var db: SQLiteDatabase? = null
        try {
            val myPath = DB_PATH + DB_NAME
            db = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: SQLiteException) {
            println("...........")
            println(e)
        }
        db?.close()
        return if (db != null) true else false
    }

    @Throws(IOException::class)
    private fun copyDataBase() {
        val myInput =
            myContext.assets.open(DB_NAME)
        val outFileName = DB_PATH + DB_NAME
        val myOutput: OutputStream = FileOutputStream(outFileName)
        val buffer = ByteArray(10)
        var length: Int
        while (myInput.read(buffer).also { length = it } > 0) {
            myOutput.write(buffer, 0, length)
        }
        myOutput.flush()
        myOutput.close()
        myInput.close()
    }

    @Throws(SQLException::class)
    fun openDataBase() {
        val myPath = DB_PATH + DB_NAME
        myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY)
    }

    @Synchronized
    override fun close() {
        if (myDataBase != null) myDataBase!!.close()
        super.close()
    }

    override fun onCreate(db: SQLiteDatabase) {}
    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (newVersion > oldVersion) try {
            copyDataBase()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun query(
        table: String?,
        columns: Array<String?>?,
        selection: String?,
        selectionArgs: Array<String?>?,
        groupBy: String?,
        having: String?,
        orderBy: String?
    ): Cursor {
        return myDataBase!!.query(
            "Categories",
            null,
            null,
            null,
            null,
            null,
            null
        )
    }

    companion object {
        private const val DB_NAME = "BrickList"
    }

    init {
        DB_PATH = "/data/data/${myContext.packageName}/databases/"
        Log.e("Path 1", DB_PATH)
    }

}