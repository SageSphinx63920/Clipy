package de.sage.clipy.audio;

import de.sage.clipy.sql.LiteSQL;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.audio.UserAudio;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class AudioHandler implements AudioSendHandler, AudioReceiveHandler {
    private static final HashMap<Long, List<byte[]>> userInput = new HashMap<>();

    /* Receive Handling */
    @Override
    public boolean canReceiveCombined() {
        return false; //We only use user audio
    }

    //Commented out since we only use user audio
    /*@Override
    public void handleCombinedAudio(CombinedAudio combinedAudio) {
        // we only want to send data when a user actually sent something, otherwise we would just send silence
        if (combinedAudio.getUsers().isEmpty())
            return;

        byte[] data = combinedAudio.getAudioData(1.0f); // volume at 100% = 1.0 (50% = 0.5 / 55% = 0.55)
        queue.add(data);
    }*/


    //Yes we want to get user audio
    @Override
    public boolean canReceiveUser() {
        return true;
    }

    @Override
    public void handleUserAudio(UserAudio userAudio) {
        if (userAudio.getUser().isBot()) return;

        //User is in list while recording, then removed
        if (userInput.containsKey(userAudio.getUser().getIdLong())) {
            List<byte[]> list = userInput.get(userAudio.getUser().getIdLong());
            list.add(userAudio.getAudioData(1));

            //50(20ms*50=1sec) packets for one seconds
            if (list.size() > 50 * 180) {
                list.remove(0);
            }
            userInput.replace(userAudio.getUser().getIdLong(), list);
        } /*else
            userInput.put(userAudio.getUser().getIdLong(), new ArrayList<>(Collections.singleton(userAudio.getAudioData(1))));*/
    }


    /* Send Handling */
    @Override
    public boolean canProvide() {
        return false; //We don't need to send anything
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        return null; // We don't send anything
    }

    @Override
    public boolean isOpus() {
        return false; //Doesn't matter since we don't send anything
    }


    /* Methods for the audio */

    /**
     * Get the list of bytes from the last 3 minutes of the user. Uses JDA's {@link net.dv8tion.jda.api.audio.AudioReceiveHandler}'s audio format
     *
     * @param user The user to get the audio from
     * @return List of byte arrays in JDA's audio format. Can be empty
     */
    @NotNull
    private static List<byte[]> getAudio(User user) {
        return userInput.get(user.getIdLong());
    }

    /**
     * Start recording the user to the HashMap
     *
     * @param user The user to record
     */
    public static void stopRecordingUser(User user) {
        userInput.remove(user.getIdLong());
    }

    /**
     * Stop recording the user and remove it from the HashMap
     *
     * @param user The user to stop recording
     */
    public static void startRecordingUser(User user) {
        ResultSet rs = LiteSQL.onQuery("SELECT * FROM userData WHERE userID = ?", user.getIdLong());

        try {
            if(rs.next()){
                if(rs.getBoolean("muted")) {
                    return;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        userInput.put(user.getIdLong(), new ArrayList<>());
    }

    /**
     * Returns if the user gets currently recorded
     *
     * @param user The user to check
     * @return true if the user gets recorded
     */
    public static boolean isRecording(User user) {
        return userInput.containsKey(user.getIdLong());
    }

    /**
     * Gets the data dir of an user to save the audio file to
     *
     * @param guild The guild to get the data dir from
     * @param user The user to get the data dir from
     * @return The data dir for the user
     */
    @NotNull
    public static File getDataDir(Guild guild, User user){
        File dataDir =  Path.of("data", guild.getId(), "audio", user.getId()).toFile();
        dataDir.mkdirs();

        return dataDir;
    }


    /**
     * Get the audio from the user as a wav file
     *
     * @param to The file to save the audio to
     * @param user The user to get the audio from
     * @param offset The offset in seconds at the start of audio
     * @return The file with the audio
     * @throws OutOfMemoryError If the audio is too big
     */
    @Nullable
    public static File getWavFile(File to, User user, int offset){
        try {
            to.createNewFile();

            List<byte[]> bytes = getAudio(user);
            int size = 0;
            for (byte[] bs : bytes) {
                size += bs.length;
            }
            byte[] decodedData = new byte[size];
            int i = 0;
            for (byte[] bs : bytes) {
                for (int j = offset*50; j < bs.length; j++) {
                    decodedData[i++] = bs[j];
                }
            }

            convertWavFile(to, decodedData);
            return to;
        } catch (IOException ignored) {
        } catch (OutOfMemoryError ignored) {
           to.delete();
        }

        return null;
    }

    /**
     * Convert the byte array to a wav file
     *
     * @param outFile The file to save the audio to
     * @param decodedData The byte array to convert
     * @throws IOException If the file can't be created
     */
    private static void convertWavFile(File outFile, byte[] decodedData) throws IOException {
        // 16 bit, 2 channels, 48kHz, signed, big endian => JDA's audio format
        AudioFormat format = new AudioFormat(48000, 16, 2, true, true);
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(
                decodedData), format, decodedData.length), AudioFileFormat.Type.WAVE, outFile);
    }
}