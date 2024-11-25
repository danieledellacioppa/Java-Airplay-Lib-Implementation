public class SAFActivity extends AppCompatActivity
{
    private static final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 1;

    Button readButton;
    Button writeButton;

    private SharedPreferenceComponent sharedPreferenceComponent;
    @Inject
    SharedPreferences sharedPreferences;

    MyDevicePolicyManager myDevicePolicyManager;

    TextView questLine;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saf);
        questLine = findViewById(R.id.quest);

        getInjectedDependencies();
        askPermissionToStorageAndSetButtons();

        usbAllowed();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("loginComplete", false);
        editor.apply();
    }

    private void usbAllowed() {
        myDevicePolicyManager = new MyDevicePolicyManager(this,this);
        myDevicePolicyManager.allowUsbFileTransfer();
        storeIntoSharedPref("usbToggle",true);
    }

    private void askPermissionToStorageAndSetButtons()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_EXTERNAL_STORAGE_PERMISSION);
        }
        else
        {
            questLine.setText("SAF is ready");

            readButton = findViewById(R.id.readButton);

            readButton.setOnClickListener(v -> {
                selectFileFromUSB();
            });

            writeButton = findViewById(R.id.writeButton);

            writeButton.setOnClickListener(v -> {
                createFileOnUSB();
            });

        }
    }

    private void getInjectedDependencies() {
        // DaggerSharedPreferenceComponent class is generated
        sharedPreferenceComponent = DaggerSharedPreferenceComponent.builder().sharedPreferenceModule(
                new SharedPreferenceModule(this)).build();

        // we are injecting the shared preference dependent object
        sharedPreferenceComponent.inject(this);
    }

    private void storeIntoSharedPref(String valueName, boolean value)
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(valueName, value);
        editor.apply();
    }

    // Override del metodo onRequestPermissionsResult per gestire la risposta dell'utente
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_EXTERNAL_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Il permesso di accesso all'archiviazione esterna è stato concesso, puoi proseguire con la logica di SAF
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                questLine.setText("Permission granted");
            } else {
                // Il permesso di accesso all'archiviazione esterna è stato negato, puoi gestire questo caso di conseguenza
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                questLine.setText("Permission denied");
            }
        }
    }

    private static final int REQUEST_PICK_FILE = 2;

    private void selectFileFromUSB() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*"); // Tutti i tipi di file
        startActivityForResult(intent, REQUEST_PICK_FILE);
    }

    // Override del metodo onActivityResult per gestire il risultato dell'intento di selezione del file
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_FILE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri selectedFileUri = data.getData();
                // Puoi ottenere il percorso del file selezionato con selectedFileUri.getPath() o utilizzare l'URI direttamente
                // Qui puoi implementare la logica di challenge password per accedere al file selezionato
                Toast.makeText(this, "File selected: " + selectedFileUri.getPath(), Toast.LENGTH_SHORT).show();
                questLine.setText("File selected: " + selectedFileUri.getPath());


                savePermanentUriToSharedPref(selectedFileUri);


                // Leggi il contenuto del file
                String fileContent = readFileContent(selectedFileUri);
                // Mostra il contenuto a video
                showFileContentAndChallenge(fileContent);

            }
        }
        else if (requestCode == REQUEST_CREATE_FILE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri createdFileUri = data.getData();
                // Puoi ottenere il percorso del file creato con createdFileUri.getPath() o utilizzare l'URI direttamente
                // Qui puoi implementare la logica per scrivere il contenuto nel file creato
                writeContentToUSB(createdFileUri, "Questo è il contenuto del file da scrivere malamente");
            }
        }
    }

    private void savePermanentUriToSharedPref(Uri selectedFileUri)
    {
        // Salvataggio dell'URI nella SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("uriKey", selectedFileUri.toString());
        editor.apply();
    }

    public String readFileContent(Uri fileUri) {
        StringBuilder content = new StringBuilder();
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }

    private void showFileContentAndChallenge(String content) {
        Toast.makeText(this, "File content:\n" + content, Toast.LENGTH_LONG).show();
        questLine.setText("File content:\n" + content);

        if(matchPassword(content))
            declareLoginComplete();
        else
            Toast.makeText(this, "Wrong key", Toast.LENGTH_SHORT).show();
    }

    private boolean matchPassword(String content) {

        return content.equals("diobellissimo\n");
    }

    private void declareLoginComplete()
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("loginComplete", true);
        editor.apply();

        onBackPressed();
    }

    private static final int REQUEST_CREATE_FILE = 3;

    private void createFileOnUSB() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain"); // Tipo di file da creare (puoi cambiare questo valore in base al tipo di file che vuoi creare)
        intent.putExtra(Intent.EXTRA_TITLE, "my_file.txt"); // Specifica un nome predefinito per il file
        startActivityForResult(intent, REQUEST_CREATE_FILE);
    }

    private void writeContentToUSB(Uri fileUri, String content) {
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(fileUri);
            if (outputStream != null) {
                outputStream.write(content.getBytes());
                outputStream.close();
                Toast.makeText(this, "File scritto correttamente!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Errore durante la scrittura del file.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed()
    {
        myDevicePolicyManager.disallowUsbFileTransfer();
        storeIntoSharedPref("usbToggle",false);
        Toast.makeText(this, "Please detach USB to secure it", Toast.LENGTH_SHORT).show();
        questLine.setText("Please detach USB to secure it");

        super.onBackPressed();
    }
}
