export default function handler(req, res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    const supabaseUrl = process.env.SUPABASE_URL || process.env.NEXT_PUBLIC_SUPABASE_URL || 'https://huokirjroudttbmwjlma.supabase.co';
    const supabaseAnonKey = process.env.SUPABASE_ANON_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imh1b2tpcmpyb3VkdHRibXdqbG1hIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTM5MzU1MjcsImV4cCI6MjEwOTUxMTUyN30.ij2UmmzBDB-oUos1nTkB6xu_VnJaaDL6JNyMRKRvO5c';

    res.status(200).json({
        supabaseUrl,
        supabaseAnonKey
    });
}

