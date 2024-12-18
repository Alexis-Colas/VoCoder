import java.sql.SQLOutput;
import java.util.ArrayList;

import static java.lang.System.exit;

public class Pauvocoder {

    // Processing SEQUENCE size (100 msec with 44100Hz samplerate)
    final static int SEQUENCE = StdAudio.SAMPLE_RATE/10;

    // Overlapping size (20 msec)
    final static int OVERLAP = SEQUENCE/5 ;
    // Best OVERLAP offset seeking window (15 msec)
    final static int SEEK_WINDOW = 3*OVERLAP/4;

    public static void main(String[] args) {
        if (args.length < 2)
        {
            System.out.println("usage: pauvocoder <input.wav> <freqScale>\n");
            exit(1);
        }


        String wavInFile = args[0];
        double freqScale = Double.valueOf(args[1]);
        String outPutFile= wavInFile.split("\\.")[0] + "_" + freqScale +"_";

        // Open input .wev file
        double[] inputWav = StdAudio.read(wavInFile);

        // Resample test
        double[] newPitchWav = resample(inputWav, freqScale);
        StdAudio.save(outPutFile+"Resampled.wav", newPitchWav);

        // Simple dilatation
        double[] outputWav  = vocodeSimple(newPitchWav, 1.0/freqScale);
        StdAudio.save(outPutFile+"Simple.wav", outputWav);

        // Simple dilatation with overlaping
        outputWav = vocodeSimpleOver(newPitchWav, 1.0/freqScale);
        StdAudio.save(outPutFile+"SimpleOver.wav", outputWav);

        // Simple dilatation with overlaping and maximum cross correlation search
        outputWav = vocodeSimpleOverCross(newPitchWav, 1.0/freqScale);
        StdAudio.save(outPutFile+"SimpleOverCross.wav", outputWav);

        joue(outputWav);

        // Some echo above all
        outputWav = echo(outputWav, 100, 0.7);
        StdAudio.save(outPutFile+"SimpleOverCrossEcho.wav", outputWav);

        // Display waveform
        displayWaveform(outputWav);


    }

    /**
     * Resample inputWav with freqScale
     * @param inputWav
     * @param freqScale
     * @return resampled newWav
     */
    public static double[] resample(double[] inputWav, double freqScale) {
        // Vérifie que "freqScale" ne soit pas égale à 0 ou inférieur.
        if (freqScale <= 0) {
            throw new IllegalArgumentException("freqScale doit être strictement positif.");
        }

        // Diviser la longueur du tableau "inputWav"
        // par la fréquence pour obtenir la taille du nouveau tableau.
        int tailleNewWav = (int) (inputWav.length / freqScale);
        double[] newWav = new double[tailleNewWav];

        // Variable qui permet d'aller chercher la valeur dans "inputWav"
        int indiceInit;
        // Boucle pour remplir le nouveau tableau "newWav"
        for (int newIndice = 0; newIndice < tailleNewWav; newIndice++) {
            // Calcule l'indice de "inputWav"
            // pour savoir la valeur qui va à l'indice "newIndice" dans "newWav"
            indiceInit = (int) (newIndice * freqScale);
            // Place la valeur a la position "newIndice" dans "newWav"
            newWav[newIndice] = inputWav[indiceInit];
        }
        // VOIR SCHEMA i=placement
        return newWav;

    }

    /**
     * Simple dilatation, without any overlapping
     * @param inputWav
     * @param dilatation factor
     * @return dilated dilatedWav
     */
    public static double[] vocodeSimple(double[] inputWav, double dilatation) {
        //message d'erreur si la valeur est négative ou égale à 0
        if (dilatation <=0)
            throw new UnsupportedOperationException("La dilatation ne peut pas être négative ou égale à 0.");

        if (dilatation == 1)
            return inputWav;

        ArrayList<Double> sequence = new ArrayList<>();
        int step = (int) (SEQUENCE * dilatation);

        for (int i = 0; i <= inputWav.length - SEQUENCE; i += step) {
            for (int j = 0; j < SEQUENCE; j++)
                sequence.add(inputWav[i + j]);
        }

        double[] dilatedWav = new double[sequence.size()];

        for (int i = 0; i < sequence.size(); i++)
            dilatedWav[i] = sequence.get(i);


        return dilatedWav;
    }

    /**
     * Simple dilatation, with overlapping
     * @param inputWav
     * @param dilatation factor
     * @return dilated wav
     */
    public static double[] vocodeSimpleOver (double[] inputWav, double dilatation) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Simple dilatation, with overlapping and maximum cross correlation search
     * @param inputWav
     * @param dilatation factor
     * @return dilated wav
     */
    public static double[] vocodeSimpleOverCross(double[] inputWav, double dilatation) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Play the wav
     * @param wav
     */
    public static void joue(double[] wav) {
        for(int i=0; i<wav.length; i++){
            StdAudio.play(wav[i]);
        }
    }

    /**
     * Add an echo to the wav
     * @param wav
     * @param delay in msec
     * @param gain
     * @return  echo echoWav
     */
    public static double[] echo(double[] wav, double delay, double gain) {
        // s'assurer que delay et gain sont contenus et justes
        if (gain<0 || gain>1)
            throw new UnsupportedOperationException("L'attenuation doit être contenu entre 0 et 1");
        if (delay <0 )
            throw new UnsupportedOperationException("Le delay ne peut pas être négatif");

        //prendre chaque échantillons et ajouter un retard -> les mettre dans nouveau tableau echoWav
        //échantillon retardé = délais en ms * fréquence d'échantillonage /1000
        int delayIndice = (int) ((delay * wav.length)/1000);
        double echoWav[] = new double[wav.length];
        for (int i = 0; i<(wav.length + delay); i++) {
            echoWav[i] = wav[i - delayIndice] * gain;

            //garder amplitude de -1/1
            if (echoWav[i] > 1.0)
                echoWav[i] = 1.0;
            if(echoWav[i] < -1.0)
                echoWav[i] = -1.0;

        }
        return echoWav;
    }

    /**
     * Display the waveform
     * @param wav
     */
    public static void displayWaveform(double[] wav) {
        throw new UnsupportedOperationException("Not implemented yet");
    }


}
