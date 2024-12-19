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

        //créer une liste qui contiendra tous les éléments
        ArrayList<Double> sequence = new ArrayList<>();
        //un saut permettant d'avoir l'indice de début de la sequence, influé par la dilatation
        int saut = (int) (SEQUENCE * dilatation);

        //boucle qui copie les éléments de la sequence du tableaux initial dans la liste
        for (int i = 0; i <= inputWav.length - SEQUENCE; i += saut) {
            for (int j = 0; j < SEQUENCE; j++)
                sequence.add(inputWav[i + j]);
        }

        //initalisation du tableau final dilaté et copie des éléments de la liste au tableau
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

        //message d'erreur si la valeur est négative ou égale à 0
        if (dilatation <=0)
            throw new UnsupportedOperationException("La dilatation ne peut pas être négative ou égale à 0.");

        if (dilatation == 1)
            return inputWav;

        //créer une liste qui contiendra tous les éléments
        ArrayList<Double> sequence = new ArrayList<>();
        //un saut permettant d'avoir l'indice de début de la sequence, influé par la dilatation
        int saut = (int) (SEQUENCE * dilatation);


        for (int i = 0; i <= inputWav.length - SEQUENCE; i += saut) {

            //boucle qui sur l'overlap du début, ajout du coef pondéré
            for (int j = 0; j < OVERLAP; j++) {
                double coeffMonte = (double) j / OVERLAP;
                sequence.add(inputWav[i+j] * coeffMonte);
            }
            //boucle du milieu de sequence, sans changement
            for (int j = OVERLAP; j < SEQUENCE-OVERLAP; j++)
                sequence.add(inputWav[i + j]);

            //boucle de l'overlap de fin
            for (int j = SEQUENCE-OVERLAP; j < SEQUENCE; j++) {
                double coeffDescend = (double) (SEQUENCE - j - 1) / OVERLAP;
                sequence.add(inputWav[i+j] *coeffDescend);
            }

        }

        //initalisation du tableau final dilaté et copie des éléments de la liste au tableau
        double[] dilatedWav = new double[sequence.size()];
        for (int i = 0; i < sequence.size(); i++)
            dilatedWav[i] = sequence.get(i);


        return dilatedWav;
    }

    /**
     * Simple dilatation, with overlapping and maximum cross correlation search
     * @param inputWav
     * @param dilatation factor
     * @return dilated wav
     */
    public static double[] vocodeSimpleOverCross(double[] inputWav, double dilatation) {

        //message d'erreur si la valeur est négative ou égale à 0
        if (dilatation <=0)
            throw new UnsupportedOperationException("La dilatation ne peut pas être négative ou égale à 0.");

        if (dilatation == 1)
            return inputWav;

        //créer une liste qui contiendra tous les éléments
        ArrayList<Double> sequence = new ArrayList<>();
        //un saut permettant d'avoir l'indice de début de la sequence, influé par la dilatation
        int saut = (int) (SEQUENCE * dilatation);

        for (int i = 0; i <= inputWav.length - SEQUENCE; i += saut) {

            //determiner le décalage optimal
            //initialiser un int optimal
            int offsetOptimal = 0;

            //boucle pour evaluer les décalage possibles
            //de 0 à la longueur de seekwindow
            for (int offset =0; offset < SEEK_WINDOW; offset ++){
                //initialiser à 0 la coorélation croisée
                double coorelation =0.0;

            }



            //boucle j=0 à overlap
            //marquer les indice de sequence précédente et suivante
            //vérifier si dépassement tableau ou mettre boucle moins 1 de base
            //pour ces indice la coorélation est indice prec * indice suiv
            // si la coorélation est supérieur à int optimal alors int optimal devient la coorélation




            //boucle qui sur l'overlap du début, ajout du coef pondéré
            for (int j = 0; j < OVERLAP; j++) {
                double coeffMonte = (double) j / OVERLAP;
                sequence.add(inputWav[i+j] * coeffMonte);
            }
            //boucle du milieu de sequence, sans changement
            for (int j = OVERLAP; j < SEQUENCE-OVERLAP; j++)
                sequence.add(inputWav[i + j]);

            //boucle de l'overlap de fin
            for (int j = SEQUENCE-OVERLAP; j < SEQUENCE; j++) {
                double coeffDescend = (double) (SEQUENCE - j - 1) / OVERLAP;
                sequence.add(inputWav[i+j] *coeffDescend);
            }

        }

        //initalisation du tableau final dilaté et copie des éléments de la liste au tableau
        double[] dilatedWav = new double[sequence.size()];
        for (int i = 0; i < sequence.size(); i++)
            dilatedWav[i] = sequence.get(i);


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
        // s'assurer que delay et gain sont contenus et justes
        if (gain<0 || gain>1)
            throw new UnsupportedOperationException("L'attenuation doit être contenu entre 0 et 1");
        if (delay <0 )
            throw new UnsupportedOperationException("Le delay ne peut pas être négatif");

        // Calcule le nombre d'échantillon que représente delay
        int nbEchantillonDelay = (int)(StdAudio.SAMPLE_RATE*delay)/1000;
        double echoWav[] = new double[wav.length+nbEchantillonDelay];

        // Ajoute l'échantillon avec le delay et le gain
        for(int i = 0; i < wav.length; i++){
            echoWav[i+nbEchantillonDelay] = wav[i]*gain;
        }

        // Ajoute le son de base a l'echo.
        for (int i = 0; i<wav.length; i++) {
            echoWav[i] += wav[i];

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

        int taille = wav.length;
        int tabSeqTaille = 4000;
        double pas = 0.8/tabSeqTaille; // 0.8 est la longueur de l'affichage
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
