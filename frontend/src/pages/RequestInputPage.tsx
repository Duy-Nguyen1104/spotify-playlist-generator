import { useState } from "react";
import { useNavigate } from "react-router-dom";
import type { PlaylistGenerationRequest } from "../types";

interface RequestInputPageProps {
  onRequestData: (req: PlaylistGenerationRequest) => void;
}

export default function RequestInputPage({ onRequestData }: RequestInputPageProps) {
  const [userRequest, setUserRequest] = useState("");
  const navigate = useNavigate();

  const handleGenerate = () => {
    if (userRequest.trim().length < 3) return;
    onRequestData({ userRequest: userRequest.trim() });
    navigate("/playlist");
  };

  return (
    <div className="row justify-content-center">
      <div className="col-md-8 col-lg-6">
        <div className="card-mood p-4 mt-3">
          <h2 className="h4 fw-bold mb-1">What do you want to listen to?</h2>
          <p className="text-secondary small mb-4">
            Describe any playlist in your own words — genre, language, mood, era, artists, occasion, or anything you
            like.
          </p>

          <textarea
            className="form-control mood-textarea mb-3"
            rows={5}
            placeholder="e.g. Korean R&B songs for a chill evening, Vietnamese study music, road trip playlist US and UK artists only..."
            value={userRequest}
            onChange={(e) => setUserRequest(e.target.value)}
            maxLength={500}
          />

          <div className="d-flex justify-content-between align-items-center mb-3">
            <small className="text-secondary">{userRequest.length}/500 characters</small>
            {userRequest.length < 3 && userRequest.length > 0 && (
              <small className="text-warning">At least 3 characters required</small>
            )}
          </div>

          <div className="d-flex gap-2 flex-wrap">
            <button
              className="btn-spotify btn flex-grow-1"
              onClick={handleGenerate}
              disabled={userRequest.trim().length < 3}
            >
              🎵 Generate Playlist
            </button>
          </div>

          <div className="mt-4">
            <p className="text-secondary small mb-2">Try these examples:</p>
            <div className="d-flex flex-wrap gap-2">
              {[
                "Korean R&B songs trending",
                "Vietnamese study music, lo-fi vibes",
                "Road trip playlist, US and UK artists only",
                "Top J-pop hits, upbeat and energetic",
                "Spanish reggaeton party bangers",
                "Sad indie folk, heartbreak healing",
              ].map((example) => (
                <button
                  key={example}
                  className="btn btn-sm btn-outline-secondary"
                  onClick={() => setUserRequest(example)}
                  style={{ fontSize: "0.75rem" }}
                >
                  {example}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
