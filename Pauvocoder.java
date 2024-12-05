public class Pauvocoder {

    /**
     * ré-echantillonner le signal audio
     * @param input
     * @param freqScale
     * @return
     */
    public static double[] resample(double[] input, double freqScale){

    }

    /**
     *ajoute un echo au signal d'entrée
     * @param input
     * @param delayMs
     * @param attn
     * @return
     */
    public static double[] echo(double[] input, double delayMs, double attn){

    }

    /**
     *impémente une dilatation temporeille dans une vesion minimal
     * @param input
     * @param timeScale
     * @return
     */
    public static double[] vocodeSimple(double[] input, double timeScale){

    }

    /**
     *dilatation temporelle sans clics
     * @param input
     * @param timeScale
     * @return
     */
    public static double[] vocodeSimpleOver(double[] input, double timeScale){

    }

    /**
     * dilatation temporelle sans clics et avec le maximum de coorélation
     * @param input
     * @param timeScale
     * @return
     */
    public static double[] vocodeSimpleOverCross(double[] input, double timeScale){

    }

    /**
     *envoie la sortie audio les données contenues dans le tableau input
     * et produit un affichage des echantillons jouées en temps réel
     * @param input
     */
    public static void joue(double[] input){

    }

    public static void main(Strings[] agrs){

    }

}