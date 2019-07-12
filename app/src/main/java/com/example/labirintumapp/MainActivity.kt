class MainActivity : AppCompatActivity() , SensorEventListener {
    init{
        private val txtEixoX: TextView         = findViewById(R.id.txtEixoX)
        private val txtEixoY: TextView         = findViewById(R.id.txtEixoY)
        private val txtEixoZ: TextView         = findViewById(R.id.txtEixoZ)
        private val btnIniciar: TextView       = findViewById(R.id.btnIniciar)
        private val mainlayout: RelativeLayout = findViewById(R.id.main_layout)

        // Atributos do Acelerômetro
        private var sensorManager: SensorManager? = null
        private var firstIter: Boolean = false
        private var eixoX0: Double = 0.0
        private var eixoY0: Double = 0.0
        private var eixoZ0: Double = 0.0
        private var eixoX: Double = 0.0
        private var eixoY: Double = 0.0
        private var eixoZ: Double = 0.0
    }
    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_principal)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager!!.registerListener(this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL)
       

        btnIniciar.setOnClickListener {
            val bSetFilename = AlertDialog.Builder(this)
            bSetFilename.setTitle("Qual é o nome do arquivo?")

            // inflate layout (?)
            val input = EditText(this)
            val lp2 = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
            lp2.leftMargin = 20
            input.layoutParams = lp
            bSetFilename.setView(input)

            bSetFilename.setPositiveButton("Iniciar", object : DialogInterface.OnClickListener() {
                override fun onClick(dialog: DialogInterface, id: Int) {
                    var filename = input.text.toString()
                    if (!filename.endsWith(".csv", true)) filename += ".csv"
                    iniciarGravacao(filename, "csv", "N")
                }
            })

            bSetFilename.show()
        }
    }

    override fun onAccuracyChanged(arg0: Sensor, arg1: Int){ }

    override fun onSensorChanged(event: SensorEvent){
        val eixoX1 = event.values[0].toDouble()
        val eixoY1 = event.values[1].toDouble()
        val eixoZ1 = event.values[2].toDouble()

        if (!firstIter){
            eixoX0 = eixoX1; eixoY0 = eixoY1; eixoZ0 = eixoZ1
            firstIter = true
        }
        else{
            eixoX = Math.abs(eixoX0 - eixoX1)
            eixoY = Math.abs(eixoY0 - eixoY1)
            eixoZ = Math.abs(eixoZ0 - eixoZ1)

            eixoX0 = eixoX1
            eixoY0 = eixoY1
            eixoZ0 = eixoZ1

            txtEixoX.text = String.format("%.2f m/s²", eixoX).replace(',', '.')
            txtEixoY.text = String.format("%.2f m/s²", eixoY).replace(',', '.')
            txtEixoZ.text = String.format("%.2f m/s²", eixoZ).replace(',', '.')
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> Toast.makeText(this, "Settings", Toast.LENGTH_LONG).show()
            R.id.action_remote -> defineRemoteLayout()
            // R.id.action_speech -> defineSpeechLayout()
        }

        return true
    }

    fun iniciarGravacao(fname: String, ftype: String, gmode: String){
        val intent = Intent(applicationContext, MenuGravacao::class.java)
        intent.putExtra("fileName", fname)
        intent.putExtra("fileType", ftype)
        intent.putExtra("recMode", gmode)
        startActivity(intent)
        finish()
    }

    private fun defineRemoteLayout(){
        Toast.makeText(this, "O modo remoto foi ativado!", Toast.LENGTH_SHORT).show()
        mainLayout.removeView(btnIniciar)
        iniciarGravacao("bluetoothRec", "csv", "R")
    }


}