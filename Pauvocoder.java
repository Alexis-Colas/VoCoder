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
        //verify that freqScale isn't equal or inferior at 0
        if (freqScale <= 0) {
            throw new IllegalArgumentException("freqScale doit être strictement positif.");
        }

        // divide by the table length
        //by the frequency to obtain the size of the new modified signal table
        int tailleNewWav = (int) (inputWav.length / freqScale);
        double[] newWav = new double[tailleNewWav];

        int indiceInit;
        // initialize the new table
        for (int newIndice = 0; newIndice < tailleNewWav; newIndice++) {
            //compute the new index
            indiceInit = (int) (newIndice * freqScale);
            newWav[newIndice] = inputWav[indiceInit];
        }
        System.out.println("resample : ");
        System.out.println("input " + inputWav.length );
        System.out.println("output " + newWav.length );
        return newWav;

    }

    /**
     * Simple dilatation, without any overlapping
     * @param inputWav
     * @param dilatation factor
     * @return dilated dilatedWav
     */
    public static double[] vocodeSimple(double[] inputWav, double dilatation) {

        if (dilatation <=0)
            throw new UnsupportedOperationException("La dilatation ne peut pas être négative ou égale à 0.");

        if (dilatation == 1)
            return inputWav;

        //inisalize new list that take the variables of the input
        ArrayList<Double> sequence = new ArrayList<>();
        // the step give the index of the beginning of the sequence, influenced by the dilatation
        int saut = (int) (SEQUENCE * dilatation);

        //copie the elements of the sequence's input in the list
        for (int i = 0; i <= inputWav.length - SEQUENCE; i += saut) {
            for (int j = 0; j < SEQUENCE; j++)
                sequence.add(inputWav[i + j]);
        }

        //initalize the dilated table and copie from the list the elements
        double[] dilatedWav = new double[sequence.size()];
        for (int i = 0; i < sequence.size(); i++)
            dilatedWav[i] = sequence.get(i);

        System.out.println("vocodeSimple : ");
        System.out.println("input " + inputWav.length );
        System.out.println("output " + dilatedWav.length );

        return dilatedWav;
    }

    /**
     * Simple dilatation, with overlapping
     * @param inputWav
     * @param dilatation factor
     * @return dilated wav
     */
    public static double[] vocodeSimpleOver (double[] inputWav, double dilatation) {

        if (dilatation <=0)
            throw new UnsupportedOperationException("La dilatation ne peut pas être négative ou égale à 0.");

        if (dilatation == 1)
            return inputWav;

        ArrayList<Double> sequence = new ArrayList<>();
        int saut = (int) (SEQUENCE * dilatation);

        for (int i = 0; i <= inputWav.length - SEQUENCE; i += saut) {
            //on the first overlap, add the wheighted coefficient
            for (int j = OVERLAP/2; j < OVERLAP; j++) {
                double coeffMonte = (double) j / OVERLAP;
                sequence.add(inputWav[i+j] * coeffMonte);
            }
            //in the middle, no change
            for (int j = OVERLAP; j < SEQUENCE-OVERLAP; j++)
                sequence.add(inputWav[i + j]);

            //final overlap
            for (int j = SEQUENCE-(OVERLAP/2); j < SEQUENCE; j++) {
                double coeffDescend = (double) (SEQUENCE - j - 1) / OVERLAP;
                sequence.add(inputWav[i+j] *coeffDescend);
            }

        }

        double[] dilatedWav = new double[sequence.size()];
        for (int i = 0; i < sequence.size(); i++)
            dilatedWav[i] = sequence.get(i);

        System.out.println("vocodeSimpleOver : ");
        System.out.println("input " + inputWav.length );
        System.out.println("output " + dilatedWav.length );

        return dilatedWav;
    }

    /**
     * Simple dilatation, with overlapping and maximum cross correlation search
     * @param inputWav
     * @param dilatation factor
     * @return dilated wav
     */
    public static double[] vocodeSimpleOverCross(double[] inputWav, double dilatation) {

        if (dilatation <=0)
            throw new UnsupportedOperationException("La dilatation ne peut pas être négative ou égale à 0.");

        if (dilatation == 1)
            return inputWav;

        ArrayList<Double> sequence = new ArrayList<>();
        int saut = (int) (SEQUENCE * dilatation);

        for (int i = 0; i <= inputWav.length - SEQUENCE; i += saut) {
            //initialize variable of the perfect offset and the maximum offset to compare to
            int offsetOptimal = 0;
            double corrMax = Double.NEGATIVE_INFINITY;

            //evaluate the possible offset between the defined window
            for (int offset =0; offset < SEEK_WINDOW; offset ++){
                //initialize the crossed correlation
                double correlation =0.0;

                //compute the correlation
                for (int j=0; j<OVERLAP; j++){
                    int seqPrec = i + SEQUENCE - OVERLAP + j; //variable of the end of the previous sequence
                    int seqSuiv = i + offset + j ;           //variable of the beginning of the next sequence

                    if (seqSuiv < inputWav.length)
                        correlation += inputWav[seqPrec] * inputWav[seqSuiv];
                }
                //if the correlation is superior at the maximum correlation, then change the optimal offset
                if ( correlation>corrMax) {
                    corrMax = correlation;
                    offsetOptimal = offset;
                }

            }

            //add coeff and the optimal offset
            for (int j = OVERLAP/2; j < OVERLAP; j++) {
                double coeffMonte = (double) j / OVERLAP;
                if (i + j + offsetOptimal < inputWav.length)
                    sequence.add(inputWav[i+j + offsetOptimal] * coeffMonte);
            }
            //no coeff, juste add the offset
            for (int j = OVERLAP; j < SEQUENCE-OVERLAP; j++)
                if (i + j + offsetOptimal < inputWav.length)
                    sequence.add(inputWav[i + j + offsetOptimal]);

            //overlap of the end
            for (int j = SEQUENCE-(OVERLAP/2); j < SEQUENCE; j++) {
                double coeffDescend = (double) (SEQUENCE - j - 1) / OVERLAP;
                if (i + j + offsetOptimal < inputWav.length)
                    sequence.add(inputWav[i+j + offsetOptimal] *coeffDescend);
            }

        }
        double[] dilatedWav = new double[sequence.size()];
        for (int i = 0; i < sequence.size(); i++)
            dilatedWav[i] = sequence.get(i);

        System.out.println("vocodeSimpleOverCross : ");
        System.out.println("input " + inputWav.length );
        System.out.println("output " + dilatedWav.length );

        return dilatedWav;

    }

    /**
     * Play the wav
     * @param wav
     */
    public static void joue(double[] wav) {
        new Thread(() -> StdAudio.play(wav)).start();
        displayWaveform(wav);
    }

    /**
     * Add an echo to the wav
     * @param wav
     * @param delay in msec
     * @param gain
     * @return  echo echoWav
     */
    public static double[] echo(double[] wav, double delay, double gain) {
        //the selay and gain have to be contained
        if (gain<0 || gain>1)
            throw new UnsupportedOperationException("L'attenuation doit être contenu entre 0 et 1");
        if (delay <0 )
            throw new UnsupportedOperationException("Le delay ne peut pas être négatif");

        //compute how many samples are in the delay
        int nbEchantillonDelay = (int)(StdAudio.SAMPLE_RATE*delay)/1000;
        double echoWav[] = new double[wav.length+nbEchantillonDelay];

        //add the sample of the delay and the gain
        for(int i = 0; i < wav.length; i++){
            echoWav[i+nbEchantillonDelay] = wav[i]*gain;
        }

        //add the echo to the input signal
        for (int i = 0; i<wav.length; i++) {
            echoWav[i] += wav[i];

            //make sure to have a range of -1 and 1
            if (echoWav[i] > 1.0)
                echoWav[i] = 1.0;
            if(echoWav[i] < -1.0)
                echoWav[i] = -1.0;

        }
        System.out.println("echo : ");
        System.out.println("input " + wav.length );
        System.out.println("output " + echoWav.length );
        return echoWav;
    }

    /**
     * Display the waveform
     * @param wav
     */
    public static void displayWaveform(double[] wav) {

        int taille = wav.length;
        int tabSeqTaille = 4000;
        double pas = 0.8/tabSeqTaille; // 0.8 is the length of the display
        int seqTaille = taille/tabSeqTaille;
        double moyenne = 0.0;

        StdDraw.setYscale(-2, 2);
        StdDraw.setPenRadius(0.001);

        for(int i = 0; i < tabSeqTaille; i++){
            int start = i * seqTaille;
            int end = start + seqTaille;
            for(int j = start; j < end; j++){
                moyenne += wav[j];
            }

            moyenne /= seqTaille;
            double x = 0.1 + (i * pas);
            StdDraw.line(x, 0, x, moyenne);
            moyenne = 0;
        }
    }
    
}
