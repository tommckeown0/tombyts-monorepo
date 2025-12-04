import express from "express";
import connectDB from "../config/database";
import movieRoutes from "../routes/movie-route";
import userRoutes from "../routes/user-route";
import loginRoutes from "../routes/auth-route";
import progressRoutes from "../routes/progress-route";
import subtitleRoutes from "../routes/subtitle-route";
import dotenv from "dotenv";
import cors from "cors";
import https from "https";
import fs from "fs";

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3001;
const isWindows = process.platform === "win32";

let keyPath: string;
let certPath: string;

if (isWindows) {
	keyPath = "C:\\Users\\tommc\\Documents\\certs\\key.pem";
	certPath = "C:\\Users\\tommc\\Documents\\certs\\cert.pem";
} else {
	keyPath = "/home/tom/tombyts-backend/certs/key.pem";
	certPath = "/home/tom/tombyts-backend/certs/cert.pem";
}

const movieBasePathWin =
	process.env.MOVIE_BASE_PATH_WIN ||
	"C:\\Users\\tommc\\Documents\\Torrents\\tombyfiles";
const movieBasePathLinux =
	process.env.MOVIE_BASE_PATH_LINUX || "/mnt/windows_share";
const mediaBasePath = isWindows ? movieBasePathWin : movieBasePathLinux;

console.log(`Server OS detected as: ${process.platform}`);
console.log(`Serving static media from: ${mediaBasePath}`);

app.use(cors());

connectDB();

app.use(express.json());

app.use("/movies", movieRoutes);
app.use("/users", userRoutes);
app.use("/media", express.static(mediaBasePath));
app.use("/subs", subtitleRoutes);
app.use("/progress", progressRoutes);
app.use("/auth", loginRoutes);

app.get("/", (req, res) => {
	res.json("Hello from TypeScript and Express!");
});

const options = {
	key: fs.readFileSync(keyPath),
	cert: fs.readFileSync(certPath),
};

https
	.createServer(options, app)
	.listen({ port: Number(PORT), host: "0.0.0.0" }, () => {
		console.log(`Server running on https://0.0.0.0:${PORT}`);
	});

// app.listen(PORT, () => {
//     console.log(`Server running on http://localhost:${PORT}`);
// });
