package com.upgenicsint.phonecheck.eraser

import android.util.Log
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 * Created by farhanahmed on 18/01/2017.
 */

class SecureDeleteExtensions(private val file: File, private val algorithm: OverwriteAlgorithm) {
    var listener: OnProgressListener? = null
    private val totalFileCount: Int
    private var totalDeleted: Int = 0

    init {
        totalFileCount = calculateTotalFiles(this.file)
    }


    fun deleteAll(file: File) {
        if (totalFileCount == 0) {
            if (listener != null) {
                listener?.onResponse(file, totalDeleted, totalFileCount)
            }
            return
        }
        try {
            for (f in file.listFiles()) {
                if (f.isDirectory) {
                    deleteAll(f)
                } else {
                    totalDeleted++
                    delete(f)
                    if (listener != null) {
                        listener?.onResponse(f, totalDeleted, totalFileCount)
                    }
                    renameAndRemove(f)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (listener != null) {
                listener?.onResponse(null, totalDeleted, totalFileCount)
            }
        }

    }

    fun delete(file: File?) {

        if (file != null && file.exists() && file.canRead() && file.canWrite()) {
            when (algorithm) {
                OverwriteAlgorithm.Quick -> deleteQuick(file)
                OverwriteAlgorithm.Random -> deleteRandom(file)
                OverwriteAlgorithm.DOD_3 -> deleteDOD3(file)
            }
        }
    }

    private fun deleteQuick(f: File) {

        try {
            val fs = FileOutputStream(f)
            val inputStream = FileInputStream(f)
            val totalSize = inputStream.available()
            var size = totalSize.toLong()
            while (size > 0) {
                val bufferSize = if (size < MAX_BUFFER_SIZE) size else MAX_BUFFER_SIZE
                val buffer = ByteArray(bufferSize.toInt())
                fs.write(buffer, 0, buffer.size)
                fs.flush()
                size -= MAX_BUFFER_SIZE
            }
            inputStream.close()
            fs.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun deleteRandom(f: File) {

        try {
            val random = Random()
            val fs = FileOutputStream(f)
            val inputStream = FileInputStream(f)
            val totalSize = inputStream.available()
            var size = totalSize.toLong()
            while (size > 0) {
                val bufferSize = if (size < MAX_BUFFER_SIZE) size else MAX_BUFFER_SIZE
                val buffer = ByteArray(bufferSize.toInt())

                for (bufferIndex in 0 until bufferSize) {
                    buffer[bufferIndex.toInt()] = (random.nextInt() % 256).toByte()
                }
                fs.write(buffer, 0, buffer.size)
                fs.flush()
                size -= MAX_BUFFER_SIZE
            }
            inputStream.close()
            fs.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun deleteDOD3(f: File) {
        try {
            val pattern = ArrayList<Byte>()
            pattern.add(0x00.toByte())
            pattern.add(0xFF.toByte())
            pattern.add(0x72.toByte())
            Collections.shuffle(pattern)
            val random = Random()
            val fs = FileOutputStream(f)
            val inputStream = FileInputStream(f)
            val totalSize = inputStream.available()
            for (pass in 1..3) {
                fs.channel.position(0)
                var size = totalSize.toLong()
                while (size > 0) {

                    val bufferSize = if (size < MAX_BUFFER_SIZE) size else MAX_BUFFER_SIZE
                    val buffer = ByteArray(bufferSize.toInt())

                    if (pass != 2) {
                        for (bufferIndex in 0 until bufferSize) {
                            buffer[bufferIndex.toInt()] = pattern[pass]
                        }
                    } else {
                        for (bufferIndex in 0 until bufferSize) {
                            buffer[bufferIndex.toInt()] = (random.nextInt() % 256).toByte()
                        }
                    }
                    fs.write(buffer, 0, buffer.size)
                    fs.flush()
                    size -= MAX_BUFFER_SIZE
                }
            }
            inputStream.close()
            fs.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /*private void deleteDOD7(File file) throws IOException {
        List<Byte> pattern = new ArrayList<>();
        pattern.add((byte) 0x00);
        pattern.add((byte) 0xFF);
        pattern.add((byte) 0x72);
        pattern.add((byte) 0x96);
        pattern.add((byte) 0x00);
        pattern.add((byte) 0xFF);
        pattern.add((byte) 0x72);
        Collections.shuffle(pattern);
        for (File f : file.listFiles()) {

            if (f.isDirectory()) {
                deleteDOD3(f);
            } else {
                Log.d("MainActivity", "Using DoD3 " + f.getAbsolutePath());

                Random random = new Random();
                FileOutputStream fs = new FileOutputStream(f);
                int totalSize = new FileInputStream(f).available();
                for (int pass = 1; pass <= 7; ++pass) {
                    fs.getChannel().position(0);
                    for (long size = totalSize; size > 0; size -= MAX_BUFFER_SIZE) {

                        long bufferSize = (size < MAX_BUFFER_SIZE) ? size : MAX_BUFFER_SIZE;
                        byte[] buffer = new byte[(int) bufferSize];

                        if (pass != 2 && pass != 6) {
                            for (int bufferIndex = 0; bufferIndex < bufferSize; ++bufferIndex) {
                                buffer[bufferIndex] = pattern.get(pass);
                            }
                        } else {
                            for (int bufferIndex = 0; bufferIndex < bufferSize; ++bufferIndex) {
                                buffer[bufferIndex] = (byte) (random.nextInt() % 256);
                            }
                        }
                        fs.write(buffer, 0, buffer.length);
                        fs.flush();
                    }
                }
                fs.close();
                renameAndRemove(f);
            }
        }

    }

    private void deleteGutmann(File file) throws IOException {
        Byte[][] patternArrays = new Byte[][]{
                new Byte[]{0x55, 0x55, 0x55}, new Byte[]{(byte) 0xAA, (byte) 0xAA, (byte) 0xAA}, new Byte[]{(byte) 0x92, 0x49, 0x24}, new Byte[]{0x49, 0x24, (byte) 0x92}, new Byte[]{0x24, (byte) 0x92, 0x49},
                new Byte[]{0x00, 0x00, 0x00}, new Byte[]{0x11, 0x11, 0x11}, new Byte[]{0x22, 0x22, 0x22}, new Byte[]{0x33, 0x33, 0x33}, new Byte[]{0x44, 0x44, 0x44},
                new Byte[]{0x55, 0x55, 0x55}, new Byte[]{0x66, 0x66, 0x66}, new Byte[]{0x77, 0x77, 0x77}, new Byte[]{(byte) 0x88, (byte) 0x88, (byte) 0x88}, new Byte[]{(byte) 0x99, (byte) 0x99, (byte) 0x99},
                new Byte[]{(byte) 0xAA, (byte) 0xAA, (byte) 0xAA},
                new Byte[]{(byte) 0xBB, (byte) 0xBB, (byte) 0xBB}, new Byte[]{(byte) 0xCC, (byte) 0xCC, (byte) 0xCC}, new Byte[]{(byte) 0xDD, (byte) 0xDD, (byte) 0xDD},
                new Byte[]{(byte) 0xEE, (byte) 0xEE, (byte) 0xEE},
                new Byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, new Byte[]{(byte) 0x92, 0x49, 0x24}, new Byte[]{0x49, 0x24, (byte) 0x92}, new Byte[]{0b100100, (byte) 0x92, 0x49}, new Byte[]{0x6D, (byte) 0xB6, (byte) 0xDB},
                new Byte[]{(byte) 0xB6, (byte) 0xDB, 0x6D}, new Byte[]{(byte) 0xDB, 0x6D, (byte) 0xB6}};
        List<Byte[]> pattern = Arrays.asList(patternArrays);

        for (File f : file.listFiles()) {

            if (f.isDirectory()) {
                deleteGutmann(f);
            } else {
                Log.d("MainActivity", "Using Guttmann " + f.getAbsolutePath());

                Random random = new Random();
                FileOutputStream fs = new FileOutputStream(f);
                int totalSize = new FileInputStream(f).available();
                for (int pass = 1; pass <= 35; ++pass) {
                    for (int index = 0; index < 3; index++) {
                        fs.getChannel().position(0);

                        for (long size = totalSize; size > 0; size -= MAX_BUFFER_SIZE) {

                            long bufferSize = (size < MAX_BUFFER_SIZE) ? size : MAX_BUFFER_SIZE;
                            byte[] buffer = new byte[(int) bufferSize];

                            if (pass > 4 && pass < 32) {
                                for (int bufferIndex = 0; bufferIndex < bufferSize; ++bufferIndex) {
                                    buffer[bufferIndex] = pattern.get(pass - 5)[index];
                                }
                            } else {
                                for (int bufferIndex = 0; bufferIndex < bufferSize; ++bufferIndex) {
                                    buffer[bufferIndex] = (byte) (random.nextInt() % 256);
                                }
                            }
                            fs.write(buffer, 0, buffer.length);
                            fs.flush();
                        }
                    }
                }
                fs.close();
                renameAndRemove(f);
            }
        }
    }*/

    private fun renameAndRemove(f: File) {
        if (!f.exists() || !f.canRead()) {
            return
        }
        val renamed = File(f.parent + "/" + Random().nextLong() + "." + Random().nextLong())
        Log.d("MainActivity", renamed.absolutePath)
        if (f.renameTo(renamed)) {
            try {
                FileUtils.forceDelete(renamed)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        val count = calculateTotalFiles(file)
    }

    interface OnProgressListener {
        fun onResponse(file: File?, position: Int, total: Int)
    }

    companion object {
        val TAG = "SecureDeleteExtensions"
        val MAX_BUFFER_SIZE = 67108864L

        fun calculateTotalFiles(file: File?): Int {

            if (file == null) {
                return 0
            }
            var total = 0

            try {
                for (f in file.listFiles()) {
                    if (f.isDirectory) {
                        total += calculateTotalFiles(f)
                    } else {
                        total++
                        //totalFileCount++;
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return total
        }
    }

}
