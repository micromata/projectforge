// https://stackoverflow.com/questions/5623838/rgb-to-hex-and-hex-to-rgb

export const hexToRgb = (color) => {
    if (!color) {
        return { r: 0, g: 0, b: 0 };
    }
    // Expand shorthand form (e.g. "03F") to full form (e.g. "0033FF")
    const shorthandRegex = /^#?([a-f\d])([a-f\d])([a-f\d])$/i;
    const hex = color.replace(shorthandRegex, (m, r, g, b) => (r + r + g + g + b + b));

    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16),
    } : null;
};

export const brightnessRGB = (r, g, b) => (r * 299 + g * 587 + b * 114) / 1000;

export const blackRGB = (r, g, b) => brightnessRGB(r, g, b) > 0.5;

export const brightness = (color) => {
    const rgb = hexToRgb(color);
    return brightnessRGB(rgb.r, rgb.g, rgb.b);
};

export const black = color => brightness(color) > 180;
