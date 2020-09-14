class GSDKeyProcessing {
    int k[] = new int[2];
    GSDJavaKeyboardClient client;

    int keyTable[][] = {
	{50, 65, 4}, // A - C
	{51, 68, 4}, // D - F
	{52, 71, 4}, // G - I
	{53, 74, 4}, // J - L
	{54, 77, 4}, // M - O
	{55, 80, 5}, // P - S
	{56, 84, 4}, // T - V
	{57, 87, 5}  // W - Z
    };
    
    public GSDKeyProcessing(GSDJavaKeyboardClient client) {
	k[0] = 0;
	k[1] = 0;
        this.client = client;
    }

    public int GetVKey(int k[])	{
	if (Character.isLowerCase((char)k[0])) {
            k[0] = (int)Character.toUpperCase((char)k[0]);
	}
	if (k[0] >= 50 && k[0] <= 57)
	{
            if (k[1] == 1) {
		return keyTable[k[0] - 50][0];
            }
            if (k[1] <= keyTable[k[0] - 50][2])	{
		return keyTable[k[0] - 50][1] + k[1] - 2;
            }
	}
	return k[0];
    }

    public int HandleKey(int kc) {
	int e[] = new int[3];
	if (k[1] != 0) {
            int ch = GetVKey(k);
            client.sendKey((char)ch);
            k[0] = 0; k[1] = 0;
            return ch;
	} else if (kc <= -1 && kc >= -5) {
            int arrowkeys[] = {
				38, // UP
				40, // DOWN
				37, // LEFT
				39, // RIGHT
				41, // SELECT
                		};
				client.sendKey((char)arrowkeys[-kc - 1]);
				k[0] = 0; k[1] = 0;
				return arrowkeys[-kc - 1];
	}
	k[0] = kc;
	k[1] = 1;
	return 0;
    }
}
