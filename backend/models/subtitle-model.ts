import mongoose, { Document, Schema } from "mongoose";

interface ISubtitle extends Document {
    movieTitle: string;  // to match with movie title
    language: string;    // e.g., "en", "es", "fr"
    path: string;        // path to the subtitle file
}

const subtitleSchema: Schema = new Schema({
    movieTitle: { type: String, required: true },
    language: { type: String, required: true },
    path: { type: String, required: true },
});

// Create an index for faster lookups
subtitleSchema.index({ movieTitle: 1, language: 1 }, { unique: true });

const Subtitle = mongoose.model<ISubtitle>("Subtitle", subtitleSchema);

export default Subtitle;